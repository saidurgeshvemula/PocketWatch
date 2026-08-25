package com.ET.fileingestion.service;

import com.ET.fileingestion.dto.ParsedTransaction;
import com.ET.transaction.entity.Transaction;
import com.ET.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionNormalisationService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> normalise(
            List<ParsedTransaction> parsed,
            Long userId,
            Long fileUploadId,
            Long bankAccountId
    ) {

        java.util.Map<String, Long> dbCountCache = new java.util.HashMap<>();
        java.util.Map<String, Integer> batchSeenCount = new java.util.HashMap<>();

        return parsed.stream()
                .filter(p -> p.getTransactionDate() != null)
                .filter(p -> p.getAmount() != null && p.getAmount().signum() > 0)
                .filter(p -> p.getDescription() != null && !p.getDescription().isBlank())
                .map(p -> toTransaction(p, userId, fileUploadId, bankAccountId))
                .filter(Objects::nonNull)
                .filter(t -> !isDuplicate(t, userId, dbCountCache, batchSeenCount))
                .toList();
    }

    private String dedupKey(Transaction t, Long userId) {
        return userId + "|" + t.getTransactionDate() + "|"
                + t.getDescription().toLowerCase() + "|" + t.getAmount().toPlainString()
                + "|" + t.getType();
    }

    private Transaction toTransaction(
            ParsedTransaction parsed,
            Long userId,
            Long fileUploadId,
            Long bankAccountId
    ) {
        try {
            return Transaction.builder()
                    .userId(userId)
                    .fileUploadId(fileUploadId)
                    .bankAccountId(bankAccountId)
                    .transactionDate(parsed.getTransactionDate())
                    .description(cleanDescription(parsed.getDescription()))
                    .amount(parsed.getAmount())
                    .type(normaliseType(parsed.getRawType()))
                    .isRecurring(false)
                    .build();
        } catch (Exception e) {
            log.warn("Skipping transaction during normalisation: {}", e.getMessage());
            return null;
        }
    }

    private Transaction.TransactionType normaliseType(String rawType) {
        if (rawType == null) return Transaction.TransactionType.DEBIT;
        String upper = rawType.trim().toUpperCase();
        if (upper.startsWith("C") || upper.contains("CREDIT")) {
            return Transaction.TransactionType.CREDIT;
        }
        return Transaction.TransactionType.DEBIT;
    }

    private String cleanDescription(String description) {
        return description
                .replaceAll("\\s+", " ")
                .trim()
                .substring(0, Math.min(description.length(), 512));
    }

    private boolean isDuplicate(
            Transaction transaction,
            Long userId,
            java.util.Map<String, Long> dbCountCache,
            java.util.Map<String, Integer> batchSeenCount
    ) {
        String key = dedupKey(transaction, userId);

        long existingInDb = dbCountCache.computeIfAbsent(key, k ->
                transactionRepository.countByUserIdAndTransactionDateAndDescriptionAndAmountAndType(
                        userId,
                        transaction.getTransactionDate(),
                        transaction.getDescription(),
                        transaction.getAmount(),
                        transaction.getType()
                )
        );

        int seenSoFar = batchSeenCount.merge(key, 1, Integer::sum);

        boolean isDuplicate = seenSoFar <= existingInDb;
        if (isDuplicate) {
            log.debug("Duplicate transaction skipped: {}", transaction.getDescription());
        }
        return isDuplicate;
    }
}