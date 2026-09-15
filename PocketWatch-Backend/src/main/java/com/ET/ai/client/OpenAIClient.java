package com.ET.ai.client;

import com.ET.ai.dto.ChatMessage;
import com.ET.config.OpenAIProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIClient {

    private final RestClient restClient;
    private final OpenAIProperties openAIProperties;
    private final ObjectMapper objectMapper;

    // ================================================================
    // RATE LIMIT CONFIGURATION
    // ================================================================

    // Keep requests serialized. With the 8K TPM limit, sending several
    // requests together is much more likely to produce 429s.
    private static final long MIN_REQUEST_INTERVAL_MS = 5000L;

    // Only ONE retry for a 429 — the provider's retry/reset value is authoritative.
    private static final int MAX_RATE_LIMIT_RETRIES = 1;

    // Small safety margin after the provider's requested wait.
    private static final long RATE_LIMIT_SAFETY_MARGIN_MS = 2000L;

    private final Object rateLimitLock = new Object();
    private long lastRequestTime = 0L;

    // ================================================================
    // CONFIGURATION
    // ================================================================

    @PostConstruct
    void validateConfig() {
        if (openAIProperties.getApiKey() == null || openAIProperties.getApiKey().isBlank()) {
            log.warn("OPENAI_API_KEY is not configured. AI calls will fail.");
        }

        log.info("AI client initialized: model={}, url={}", openAIProperties.getModel(), openAIProperties.getUrl());
    }

    // ================================================================
    // CHAT
    // ================================================================

    public String chat(List<ChatMessage> messages, int maxTokens) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("AI request must contain at least one message");
        }

        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be greater than zero");
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openAIProperties.getModel());
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", maxTokens);

        // Deterministic extraction.
        requestBody.put("temperature", 0);

        // GPT-OSS: low reasoning is sufficient for transaction extraction.
        // This prevents the model from wasting most of its completion budget on reasoning.
        requestBody.put("reasoning_effort", "low");
        requestBody.put("include_reasoning", false);

        // ============================================================
        // REQUEST / 429 HANDLING
        // ============================================================

        for (int attempt = 0; attempt <= MAX_RATE_LIMIT_RETRIES; attempt++) {
            try {
                waitForGlobalRequestInterval();

                log.info("Sending AI request: model={}, maxTokens={}, attempt={}/{}",
                        openAIProperties.getModel(), maxTokens, attempt + 1, MAX_RATE_LIMIT_RETRIES + 1);

                long start = System.currentTimeMillis();

                ResponseEntity<String> response = restClient.post()
                        .uri(openAIProperties.getUrl())
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(h -> h.setBearerAuth(openAIProperties.getApiKey()))
                        .body(requestBody)
                        .retrieve()
                        .toEntity(String.class);

                long duration = System.currentTimeMillis() - start;
                log.info("AI request completed in {} ms", duration);

                return extractContent(response.getBody());

            } catch (HttpClientErrorException.TooManyRequests e) {
                // Only retry once.
                if (attempt >= MAX_RATE_LIMIT_RETRIES) {
                    log.error("AI rate limit retry exhausted.");
                    throw e;
                }

                long providerWait = parseRetryAfterMillis(e);
                long waitMillis = providerWait + RATE_LIMIT_SAFETY_MARGIN_MS;

                log.warn("AI provider returned 429. Waiting {} ms before one retry.", waitMillis);
                sleep(waitMillis);

            } catch (AIResponseTooLongException e) {
                // DO NOT wrap. The extraction service will handle this as a failed
                // chunk rather than recursively splitting forever.
                throw e;

            } catch (HttpClientErrorException e) {
                log.error("AI provider returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
                throw e;

            } catch (Exception e) {
                log.error("AI provider call failed ({}): {}", e.getClass().getSimpleName(), e.getMessage(), e);
                throw new RuntimeException("AI provider call failed: " + e.getMessage(), e);
            }
        }

        throw new RuntimeException("AI provider call failed after retry");
    }

    // ================================================================
    // GLOBAL REQUEST INTERVAL
    // ================================================================

    private void waitForGlobalRequestInterval() {
        synchronized (rateLimitLock) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRequestTime;

            if (lastRequestTime > 0 && elapsed < MIN_REQUEST_INTERVAL_MS) {
                long wait = MIN_REQUEST_INTERVAL_MS - elapsed;
                log.debug("Waiting {} ms before next AI request.", wait);
                sleep(wait);
            }

            lastRequestTime = System.currentTimeMillis();
        }
    }

    // ================================================================
    // RETRY-AFTER PARSER
    // ================================================================

    private long parseRetryAfterMillis(HttpClientErrorException e) {
        // First check the Retry-After header.
        String retryAfter = null;

        if (e.getResponseHeaders() != null) {
            retryAfter = e.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER);
        }

        if (retryAfter != null) {
            try {
                double seconds = Double.parseDouble(retryAfter.trim());
                return (long) (seconds * 1000L);
            } catch (NumberFormatException ignored) {
                log.debug("Retry-After header is not numeric: {}", retryAfter);
            }
        }

        // Groq may put: "try again in 15.6225s"
        String body = e.getResponseBodyAsString();

        if (body != null && !body.isBlank()) {
            Pattern pattern = Pattern.compile("try again in\\s+([0-9.]+)s", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(body);

            if (matcher.find()) {
                try {
                    double seconds = Double.parseDouble(matcher.group(1));
                    return (long) (seconds * 1000L);
                } catch (NumberFormatException ignored) {
                    // Fall through.
                }
            }
        }

        // If the provider gives no retry time, use a conservative 20 seconds.
        return 20000L;
    }

    // ================================================================
    // RESPONSE PARSING
    // ================================================================

    private String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new RuntimeException("AI provider returned an empty response body");
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);

            // Log usage.
            JsonNode usage = root.path("usage");

            if (!usage.isMissingNode()) {
                int promptTokens = usage.path("prompt_tokens").asInt(0);
                int completionTokens = usage.path("completion_tokens").asInt(0);
                int totalTokens = usage.path("total_tokens").asInt(0);

                log.info("AI usage: prompt={}, completion={}, total={}", promptTokens, completionTokens, totalTokens);

                JsonNode details = usage.path("completion_tokens_details");

                if (!details.isMissingNode()) {
                    int reasoningTokens = details.path("reasoning_tokens").asInt(0);

                    if (reasoningTokens > 0) {
                        log.info("AI reasoning tokens: {}", reasoningTokens);
                    }
                }
            }

            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("AI provider returned no choices");
            }

            JsonNode choice = choices.get(0);
            String finishReason = choice.path("finish_reason").asText(null);

            // IMPORTANT: don't recursively split here. A truncated response is
            // reported to the extraction service as a controlled failure.
            if ("length".equalsIgnoreCase(finishReason)) {
                log.error("AI response truncated because completion token limit was reached.");
                throw new AIResponseTooLongException(
                        "AI response truncated because completion token limit was reached");
            }

            JsonNode message = choice.path("message");
            String content = message.path("content").asText(null);

            if (content == null || content.isBlank()) {
                throw new RuntimeException("AI provider returned empty message content");
            }

            return content.trim();

        } catch (AIResponseTooLongException e) {
            throw e;

        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", responseBody, e);
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }

    // ================================================================
    // SLEEP
    // ================================================================

    private void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI request interrupted", e);
        }
    }

    // ================================================================
    // SPECIAL EXCEPTION
    // ================================================================

    public static class AIResponseTooLongException extends RuntimeException {
        public AIResponseTooLongException(String message) {
            super(message);
        }
    }
}