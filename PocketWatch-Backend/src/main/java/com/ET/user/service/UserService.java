package com.ET.user.service;

import com.ET.exception.BusinessException;
import com.ET.exception.ResourceNotFoundException;
import com.ET.security.JwtTokenProvider;
import com.ET.user.dto.*;
import com.ET.user.entity.User;
import com.ET.user.entity.VerificationToken;
import com.ET.user.repository.UserRepository;
import com.ET.user.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    // ============================================================
    // REGISTER
    // ============================================================

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email is already registered");
        }

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .isActive(true)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        log.info("New user registered: {}", user.getEmail());

        User finalUser = user;

        // Email verification token expires after 15 minutes
        issueToken(
                user,
                VerificationToken.Type.EMAIL_VERIFY,
                15,
                token -> emailService.sendVerificationEmail(
                        finalUser.getEmail(),
                        token
                )
        );

        return AuthResponse.of(
                null,
                null,
                UserResponse.from(user)
        );
    }

    // ============================================================
    // VERIFY EMAIL
    // ============================================================

    @Transactional
    public void verifyEmail(String token) {

        VerificationToken verificationToken =
                consumeToken(
                        token,
                        VerificationToken.Type.EMAIL_VERIFY
                );

        User user = userRepository.findById(
                        verificationToken.getUserId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        user.setEmailVerified(true);

        userRepository.save(user);

        log.info("Email verified: {}", user.getEmail());
    }

    // ============================================================
    // RESEND VERIFICATION EMAIL
    // ============================================================

    @Transactional
    public void resendVerification(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        if (user.isEmailVerified()) {
            return;
        }

        // Resent email verification token also expires after 15 minutes
        issueToken(
                user,
                VerificationToken.Type.EMAIL_VERIFY,
                1440,
                token -> emailService.sendVerificationEmail(
                        user.getEmail(),
                        token
                )
        );
    }

    // ============================================================
    // LOGIN
    // ============================================================

    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        if (!user.isEmailVerified()) {
            throw new BusinessException(
                    "Please verify your email before logging in."
            );
        }

        log.info("User logged in: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    // ============================================================
    // REFRESH TOKEN
    // ============================================================

    public AuthResponse refresh(String refreshToken) {

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(
                    "Invalid or expired refresh token"
            );
        }

        if (!"refresh".equals(
                jwtTokenProvider.extractTokenType(refreshToken)
        )) {
            throw new BusinessException(
                    "Invalid token type"
            );
        }

        String email =
                jwtTokenProvider.extractEmail(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        int tokenVersion = jwtTokenProvider.extractTokenVersion(refreshToken);
        if (tokenVersion != user.getTokenVersion()) {
            throw new BusinessException(
                    "Session has been revoked. Please log in again."
            );
        }

        return generateAuthResponse(user);
    }

    // ============================================================
    // LOGOUT FROM ALL DEVICES
    // ============================================================

    @Transactional
    public void logoutAllDevices(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        log.info("All tokens invalidated for user: {}", user.getEmail());
    }

    // ============================================================
    // FORGOT PASSWORD
    // ============================================================

    @Transactional
    public void forgotPassword(String email) {

        userRepository.findByEmail(email).ifPresent(user ->

                // Password reset token expires after 15 minutes
                issueToken(
                        user,
                        VerificationToken.Type.PASSWORD_RESET,
                        1440,
                        token -> emailService.sendPasswordResetEmail(
                                user.getEmail(),
                                token
                        )
                )
        );
    }

    // ============================================================
    // RESET PASSWORD
    // ============================================================

    @Transactional
    public void resetPassword(
            String token,
            String newPassword
    ) {

        VerificationToken verificationToken =
                consumeToken(
                        token,
                        VerificationToken.Type.PASSWORD_RESET
                );

        User user = userRepository.findById(
                        verificationToken.getUserId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        user.setPasswordHash(
                passwordEncoder.encode(newPassword)
        );
        // Invalidate any tokens issued before this reset
        user.setTokenVersion(user.getTokenVersion() + 1);

        userRepository.save(user);

        log.info(
                "Password reset for {}",
                user.getEmail()
        );
    }

    // ============================================================
    // GET CURRENT USER
    // ============================================================

    public UserResponse getMe(UserDetails userDetails) {

        User user = userRepository.findByEmail(
                        userDetails.getUsername()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        return UserResponse.from(user);
    }

    // ============================================================
    // ISSUE VERIFICATION / RESET TOKEN
    // ============================================================

    private void issueToken(
            User user,
            VerificationToken.Type type,
            long validMinutes,
            Consumer<String> sender
    ) {

        String token = UUID.randomUUID().toString();

        tokenRepository.save(
                VerificationToken.builder()
                        .userId(user.getId())
                        .token(token)
                        .type(type)
                        .expiresAt(
                                LocalDateTime.now()
                                        .plusMinutes(validMinutes)
                        )
                        .used(false)
                        .build()
        );

        try {

            sender.accept(token);

            log.info(
                    "{} email sent to {}",
                    type,
                    user.getEmail()
            );

        } catch (Exception e) {

            log.error(
                    "Email sending failed",
                    e
            );

            throw new BusinessException(
                    "Unable to send verification email. Please try again."
            );
        }
    }

    // ============================================================
    // CONSUME TOKEN
    // ============================================================

    private VerificationToken consumeToken(
            String token,
            VerificationToken.Type type
    ) {

        VerificationToken verificationToken =
                tokenRepository.findByTokenAndType(
                                token,
                                type
                        )
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Invalid or expired token"
                                )
                        );

        // Check whether token is already used
        // OR has passed its 24-hour expiration time
        if (verificationToken.isUsed()
                || verificationToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new BusinessException(
                    "Invalid or expired token"
            );
        }

        // Token can only be used once
        verificationToken.setUsed(true);

        tokenRepository.save(verificationToken);

        return verificationToken;
    }

    // ============================================================
    // GENERATE AUTH RESPONSE
    // ============================================================

    private AuthResponse generateAuthResponse(User user) {

        String accessToken =
                jwtTokenProvider.generateAccessToken(
                        user.getEmail(),
                        user.getId(),
                        user.getRole().name(),
                        user.getTokenVersion()
                );

        String refreshToken =
                jwtTokenProvider.generateRefreshToken(
                        user.getEmail(),
                        user.getTokenVersion()
                );

        return AuthResponse.of(
                accessToken,
                refreshToken,
                UserResponse.from(user)
        );
    }
}