package com.ET.user;

import com.ET.exception.BusinessException;
import com.ET.security.JwtTokenProvider;
import com.ET.user.dto.AuthResponse;
import com.ET.user.dto.RegisterRequest;
import com.ET.user.entity.User;
import com.ET.user.repository.UserRepository;
import com.ET.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserService userService;

    @Test
    void register_shouldSucceed_whenEmailNotTaken() {

        RegisterRequest request = new RegisterRequest("Sai Durgesh", "sai@test.com", "password123");
        when(userRepository.existsByEmail("sai@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(anyString(), anyLong(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString()))
                .thenReturn("refresh-token");

        AuthResponse response = userService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("sai@test.com");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_shouldThrowBusinessException_whenEmailAlreadyTaken() {

        RegisterRequest request = new RegisterRequest("Sai", "taken@test.com", "password123");
        when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldEncodePassword_beforeSaving() {

        RegisterRequest request = new RegisterRequest("Sai", "sai@test.com", "plainpassword");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plainpassword")).thenReturn("$2a$12$hashedvalue");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh");

        userService.register(request);

        verify(userRepository).save(argThat(u ->
                u.getPasswordHash().equals("$2a$12$hashedvalue") &&
                !u.getPasswordHash().equals("plainpassword")
        ));
    }

    @Test
    void getMe_shouldReturnUserResponse_whenUserExists() {

        User user = User.builder()
                .id(1L).email("sai@test.com").fullName("Sai Durgesh")
                .role(User.Role.USER).isActive(true).build();

        org.springframework.security.core.userdetails.UserDetails userDetails =
                mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetails.getUsername()).thenReturn("sai@test.com");
        when(userRepository.findByEmail("sai@test.com")).thenReturn(Optional.of(user));

        var result = userService.getMe(userDetails);

        assertThat(result.email()).isEqualTo("sai@test.com");
        assertThat(result.fullName()).isEqualTo("Sai Durgesh");
    }
}
