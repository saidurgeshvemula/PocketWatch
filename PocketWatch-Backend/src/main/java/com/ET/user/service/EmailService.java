package com.ET.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestClient restClient;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.from-address}")
    private String fromAddress;

    @Value("${brevo.from-name:PocketWatch}")
    private String fromName;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Async("fileProcessingExecutor")
    public void sendVerificationEmail(String to, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        send(to, "Verify your PocketWatch email",
                "Click to verify your email:\n" + link + "\n\nThis link expires in 24 hours.",
                "<p>Click below to verify your email:</p><p><a href=\"" + link + "\">Verify Email</a></p>" +
                        "<p>This link expires in 24 hours.</p>");
    }

    @Async("fileProcessingExecutor")
    public void sendPasswordResetEmail(String to, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        send(to, "Reset your PocketWatch password",
                "Click to reset your password:\n" + link +
                        "\n\nThis link expires in 1 hour. If you didn't request this, ignore this email.",
                "<p>Click below to reset your password:</p><p><a href=\"" + link + "\">Reset Password</a></p>" +
                        "<p>This link expires in 1 hour. If you didn't request this, ignore this email.</p>");
    }

    private void send(String to, String subject, String textBody, String htmlBody) {
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("email", fromAddress, "name", fromName),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlBody,
                    "textContent", textBody
            );

            restClient.post()
                    .uri(BREVO_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("api-key", brevoApiKey)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} ({}): {}", to, subject, e.getMessage());
        }
    }
}