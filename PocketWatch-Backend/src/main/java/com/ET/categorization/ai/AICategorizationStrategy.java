package com.ET.categorization.ai;

import com.ET.ai.client.OpenAIClient;
import com.ET.ai.dto.ChatMessage;
import com.ET.categorization.dto.CategorizationResult;
import com.ET.categorization.dto.CategorizationSource;
import com.ET.categorization.dto.ExtractedMerchantInfo;
import com.ET.category.entity.Category;
import com.ET.category.repository.CategoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AICategorizationStrategy {

    private final OpenAIClient openAIClient;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    private static final String FALLBACK_CATEGORY = "Other";

    private static final int MAX_ITEMS_PER_CALL = 10;

    private static final int MAX_TOKENS = 400 + (MAX_ITEMS_PER_CALL * 70);

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private static final String SYSTEM_PROMPT = """
            You are a financial transaction categorizer for Indian bank statements.

            Return ONLY a JSON array.

            Categories:
            Food & Dining,
            Groceries,
            Shopping,
            Transport,
            Healthcare,
            Utilities,
            Entertainment,
            Education,
            Travel,
            Subscriptions,
            Income,
            Investments,
            Transfers,
            Cash Withdrawal,
            Loan,
            Other

            Response format:

            [
              {
                "id":1,
                "merchant":"Swiggy",
                "category":"Food & Dining",
                "confidence":95,
                "reason":"Food delivery"
              }
            ]
            """;

    public Map<Integer, CategorizationResult> categorizeBatch(
            Map<Integer, ExtractedMerchantInfo> batch) {

        if (batch.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> categoryIdsByName =
                categoryRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                c -> c.getName().toLowerCase(),
                                Category::getId
                        ));

        Map<Integer, CategorizationResult> results = new HashMap<>();

        List<Map.Entry<Integer, ExtractedMerchantInfo>> entries =
                new ArrayList<>(batch.entrySet());

        for (int start = 0; start < entries.size(); start += MAX_ITEMS_PER_CALL) {

            List<Map.Entry<Integer, ExtractedMerchantInfo>> subBatch =
                    entries.subList(
                            start,
                            Math.min(start + MAX_ITEMS_PER_CALL, entries.size())
                    );

            results.putAll(categorizeSubBatch(subBatch, categoryIdsByName));

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return results;
    }
    private Map<Integer, CategorizationResult> categorizeSubBatch(
            List<Map.Entry<Integer, ExtractedMerchantInfo>> subBatch,
            Map<String, Long> categoryIdsByName) {

        List<AICategorizationRequestItem> requestItems =
                subBatch.stream()
                        .map(e -> toRequestItem(e.getKey(), e.getValue()))
                        .toList();

        try {

            String userPrompt = objectMapper.writeValueAsString(requestItems);

            log.debug("AI request: {}", userPrompt);

            List<ChatMessage> messages = List.of(
                    ChatMessage.system(SYSTEM_PROMPT),
                    ChatMessage.user(userPrompt)
            );

            String response = callAIWithRetry(messages);

            log.debug("AI response: {}", response);

            List<AICategorizationResponseItem> items =
                    parseResponse(response);

            Map<Integer, CategorizationResult> results =
                    new HashMap<>();

            for (AICategorizationResponseItem item : items) {

                if (item.getId() == null) {
                    continue;
                }

                int index = item.getId().intValue();

                String categoryName =
                        item.getCategory() != null
                                ? item.getCategory()
                                : FALLBACK_CATEGORY;

                Long categoryId =
                        categoryIdsByName.getOrDefault(
                                categoryName.toLowerCase(),
                                categoryIdsByName.get(
                                        FALLBACK_CATEGORY.toLowerCase()
                                )
                        );

                int confidence =
                        item.getConfidence() != null
                                ? Math.max(
                                0,
                                Math.min(100,
                                        item.getConfidence())
                        )
                                : 50;

                String reason =
                        item.getReason() != null
                                ? item.getReason()
                                : "AI categorization";

                String merchant =
                        item.getMerchant() != null &&
                                !item.getMerchant().isBlank()
                                ? item.getMerchant().trim()
                                : null;

                results.put(
                        index,
                        new CategorizationResult(
                                null,
                                categoryId,
                                categoryName,
                                confidence,
                                CategorizationSource.AI,
                                reason,
                                merchant
                        )
                );
            }

            return results;

        } catch (Exception e) {

            log.error(
                    "========== AI CATEGORIZATION FAILED ==========",
                    e
            );

            return Map.of();
        }
    }

    private String callAIWithRetry(List<ChatMessage> messages) {

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {

            try {

                return openAIClient.chat(messages, MAX_TOKENS);

            } catch (HttpClientErrorException.TooManyRequests ex) {

                long wait = retryAfterMillis(ex).orElse(RETRY_DELAY_MS * attempt);

                log.warn(
                        "Groq rate limit hit. Attempt {}/{}. Waiting {} ms.",
                        attempt,
                        MAX_RETRIES,
                        wait
                );

                if (attempt == MAX_RETRIES) {
                    throw ex;
                }

                try {
                    Thread.sleep(wait);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }

        throw new IllegalStateException("Retry failed.");
    }

    private Optional<Long> retryAfterMillis(HttpClientErrorException.TooManyRequests ex) {
        String header = ex.getResponseHeaders() != null
                ? ex.getResponseHeaders().getFirst("Retry-After")
                : null;
        if (header == null) return Optional.empty();
        try {
            return Optional.of(Long.parseLong(header.trim()) * 1000L);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
    private AICategorizationRequestItem toRequestItem(
            int index,
            ExtractedMerchantInfo info) {

        return AICategorizationRequestItem.builder()
                .id((long) index)

                .merchant(
                        info.merchantRaw() != null &&
                                !info.merchantRaw().isBlank()
                                ? info.merchantRaw()
                                : info.cleanedDescription()
                )

                .paymentMode(
                        info.paymentMode() != null
                                ? info.paymentMode().name()
                                : "UNKNOWN"
                )

                .bank(info.bankCode())

                .direction(
                        info.direction() != null
                                ? info.direction().name()
                                : null
                )

                .originalDescription(info.originalDescription())

                .build();
    }

    private List<AICategorizationResponseItem> parseResponse(String response) {

        try {

            String cleaned = response
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(
                    cleaned,
                    new TypeReference<List<AICategorizationResponseItem>>() {}
            );

        } catch (Exception e) {

            log.error("========== FAILED TO PARSE AI RESPONSE ==========", e);
            log.error("Raw response:\n{}", response);

            return List.of();
        }
    }
}