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

    @Value("${sendgrid.api-key}")
    private String sendgridApiKey;

    @Value("${sendgrid.from-address}")
    private String fromAddress;

    @Value("${sendgrid.from-name:PocketWatch}")
    private String fromName;

    private static final String SENDGRID_API_URL = "https://api.sendgrid.com/v3/mail/send";

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
                    "personalizations", List.of(Map.of(
                            "to", List.of(Map.of("email", to))
                    )),
                    "from", Map.of("email", fromAddress, "name", fromName),
                    "subject", subject,
                    "content", List.of(
                            Map.of("type", "text/plain", "value", textBody),
                            Map.of("type", "text/html", "value", htmlBody)
                    )
            );

            restClient.post()
                    .uri(SENDGRID_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> h.setBearerAuth(sendgridApiKey))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} ({}): {}", to, subject, e.getMessage());
        }
    }
}