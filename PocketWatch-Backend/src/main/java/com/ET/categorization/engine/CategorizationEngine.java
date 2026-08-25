package com.ET.categorization.engine;

import com.ET.analytics.service.AnalyticsService;
import com.ET.categorization.ai.AICategorizationStrategy;
import com.ET.categorization.dto.CategorizationResult;
import com.ET.categorization.dto.CategorizationSource;
import com.ET.categorization.dto.ExtractedMerchantInfo;
import com.ET.categorization.extraction.MerchantExtractor;
import com.ET.categorization.merchant.MerchantLookupService;
import com.ET.categorization.rules.RuleEngine;
import com.ET.category.entity.Category;
import com.ET.category.repository.CategoryRepository;
import com.ET.transaction.entity.Transaction;
import com.ET.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategorizationEngine {

    private final MerchantExtractor merchantExtractor;
    private final MerchantLookupService merchantLookupService;
    private final RuleEngine ruleEngine;
    private final AICategorizationStrategy aiCategorizationStrategy;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AnalyticsService analyticsService;

    private static final String FALLBACK_CATEGORY = "Other";

    @Transactional
    public void categorizeAndSave(List<Transaction> transactions) {
        if (transactions.isEmpty()) return;

        log.info("Starting hybrid categorization for {} transactions", transactions.size());
        categorizeBatch(transactions);
        transactionRepository.saveAll(transactions);
        log.info("Categorization complete: {} transactions categorized", transactions.size());

        analyticsService.evictUserCache(transactions.get(0).getUserId());
    }

    @Transactional
    public String recategorizeAndSave(Transaction transaction) {
        ExtractedMerchantInfo extracted = merchantExtractor.extract(transaction.getDescription());

        Optional<CategorizationResult> merchantHit = extracted.merchantFound()
                ? merchantLookupService.lookup(extracted.merchantRaw())
                : Optional.empty();

        CategorizationResult result = merchantHit.orElse(null);

        if (result == null) {
            Optional<CategorizationResult> ruleHit = ruleEngine.match(extracted);
            result = ruleHit.map(r -> resolveMerchant(r, extracted)).orElse(null);
        }

        if (result == null) {
            Map<Integer, CategorizationResult> aiResult =
                    aiCategorizationStrategy.categorizeBatch(Map.of(0, extracted));
            result = aiResult.containsKey(0)
                    ? resolveMerchant(aiResult.get(0), extracted)
                    : fallbackToOther();
        }

        apply(transaction, extracted, result);
        transactionRepository.save(transaction);
        analyticsService.evictUserCache(transaction.getUserId());
        return result.categoryName();
    }

    public void categorizeBatch(List<Transaction> transactions) {
        if (transactions.isEmpty()) return;

        Map<String, Long> categoryIdsByName = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getName().toLowerCase(), c -> c.getId()));
        Long incomeId = categoryIdsByName.get("income");

        List<Transaction> remaining = tagRecurringIncome(transactions, incomeId);

        Map<Integer, Transaction> pendingByIndex = new LinkedHashMap<>();
        Map<Integer, ExtractedMerchantInfo> extractedByIndex = new LinkedHashMap<>();
        Map<Integer, ExtractedMerchantInfo> needsAi = new LinkedHashMap<>();

        int idx = 0;
        for (Transaction t : remaining) {
            ExtractedMerchantInfo extracted = merchantExtractor.extract(t.getDescription());
            pendingByIndex.put(idx, t);
            extractedByIndex.put(idx, extracted);

            Optional<CategorizationResult> merchantHit = extracted.merchantFound()
                    ? merchantLookupService.lookup(extracted.merchantRaw())
                    : Optional.empty();

            if (merchantHit.isPresent()) {
                apply(t, extracted, merchantHit.get());
                idx++;
                continue;
            }

            Optional<CategorizationResult> ruleHit = ruleEngine.match(extracted);
            if (ruleHit.isPresent()) {
                apply(t, extracted, resolveMerchant(ruleHit.get(), extracted));
                idx++;
                continue;
            }

            needsAi.put(idx, extracted);
            idx++;
        }

        if (!needsAi.isEmpty()) {
            log.info("{} transactions need AI categorization (merchant DB + rules both missed)", needsAi.size());
            Map<Integer, CategorizationResult> aiResults = aiCategorizationStrategy.categorizeBatch(needsAi);

            Long otherId = categoryIdsByName.get(FALLBACK_CATEGORY.toLowerCase());
            for (Integer i : needsAi.keySet()) {
                Transaction t = pendingByIndex.get(i);
                ExtractedMerchantInfo extracted = extractedByIndex.get(i);
                CategorizationResult result = aiResults.get(i);

                if (result == null) {
                    result = new CategorizationResult(null, otherId, FALLBACK_CATEGORY, 0,
                            CategorizationSource.AI, "AI categorization failed; defaulted to Other", null);
                } else {
                    result = resolveMerchant(result, extracted);
                }
                apply(t, extracted, result);
            }
        }
    }

    private CategorizationResult resolveMerchant(CategorizationResult result, ExtractedMerchantInfo extracted) {
        if (result.merchantId() != null || result.categoryId() == null) return result;

        String merchantName = result.merchantName() != null && !result.merchantName().isBlank()
                ? result.merchantName()
                : extracted.merchantRaw();
        if (merchantName == null || merchantName.isBlank()) return result;

        Long merchantId = merchantLookupService.findOrCreateMerchant(
                merchantName, result.categoryId(), result.confidence());

        return new CategorizationResult(merchantId, result.categoryId(), result.categoryName(),
                result.confidence(), result.source(), result.reason(), merchantName);
    }

    private CategorizationResult fallbackToOther() {
        Long otherId = categoryRepository.findAll().stream()
                .filter(c -> FALLBACK_CATEGORY.equalsIgnoreCase(c.getName()))
                .map(Category::getId)
                .findFirst()
                .orElse(null);
        return new CategorizationResult(null, otherId, FALLBACK_CATEGORY, 0,
                CategorizationSource.AI, "AI categorization failed; defaulted to Other", null);
    }

    private List<Transaction> tagRecurringIncome(List<Transaction> transactions, Long incomeId) {
        Map<String, Long> creditDescCountsInBatch = transactions.stream()
                .filter(t -> t.getType() == Transaction.TransactionType.CREDIT)
                .collect(Collectors.groupingBy(t -> t.getDescription().toLowerCase(), Collectors.counting()));

        List<Transaction> remaining = new ArrayList<>();
        for (Transaction t : transactions) {
            boolean isRecurringCredit = incomeId != null
                    && t.getType() == Transaction.TransactionType.CREDIT
                    && (creditDescCountsInBatch.getOrDefault(t.getDescription().toLowerCase(), 0L) >= 2
                    || transactionRepository.countByUserIdAndDescriptionIgnoreCaseAndType(
                    t.getUserId(), t.getDescription(), Transaction.TransactionType.CREDIT) > 0);

            if (isRecurringCredit) {
                t.setCategoryId(incomeId);
                t.setCategorySource(CategorizationSource.RULE_ENGINE);
                t.setCategoryConfidence(CategorizationResult.RULE_ENGINE_CONFIDENCE);
                t.setCategoryReason("Recurring credit auto-tagged as Income");
            } else {
                remaining.add(t);
            }
        }
        log.info("{} transactions auto-tagged as recurring Income, {} left for categorization",
                transactions.size() - remaining.size(), remaining.size());
        return remaining;
    }

    private void apply(Transaction t, ExtractedMerchantInfo extracted, CategorizationResult result) {
        t.setCategoryId(result.categoryId());
        t.setCategorySource(result.source());
        t.setCategoryConfidence(result.confidence());
        t.setCategoryReason(result.reason());
        t.setMerchantId(result.merchantId());

        if (t.getMerchantName() == null || t.getMerchantName().isBlank()) {
            if (result.merchantName() != null && !result.merchantName().isBlank()) {
                t.setMerchantName(result.merchantName());
            } else if (extracted.merchantFound()) {
                t.setMerchantName(extracted.merchantRaw());
            }
        }
    }
}
