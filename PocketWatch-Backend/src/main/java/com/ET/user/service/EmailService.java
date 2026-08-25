package com.ET.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Async("fileProcessingExecutor")
    public void sendVerificationEmail(String to, String token) {
        send(to, "Verify your PocketWatch email",
                "Click to verify your email:\n" + frontendUrl + "/verify-email?token=" + token +
                        "\n\nThis link expires in 24 hours.");
    }

    @Async("fileProcessingExecutor")
    public void sendPasswordResetEmail(String to, String token) {
        send(to, "Reset your PocketWatch password",
                "Click to reset your password:\n" + frontendUrl + "/reset-password?token=" + token +
                        "\n\nThis link expires in 1 hour. If you didn't request this, ignore this email.");
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        try {
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {

            log.error("Failed to send email to {} ({}): {}", to, subject, e.getMessage());
        }
    }
}