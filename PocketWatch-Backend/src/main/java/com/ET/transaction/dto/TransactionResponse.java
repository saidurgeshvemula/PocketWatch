package com.ET.transaction.dto;

import com.ET.transaction.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransactionResponse(

        Long id,

        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate transactionDate,

        String description,

        BigDecimal amount,

        String type,

        String source,

        Long categoryId,

        Long bankAccountId,

        String merchantName,

        Long merchantId,

        String categorySource,

        Integer categoryConfidence,

        String categoryReason,

        boolean isRecurring,

        String notes,

        LocalDateTime createdAt

) {

    public static TransactionResponse from(Transaction t) {

        return new TransactionResponse(

                t.getId(),

                t.getTransactionDate(),

                t.getDescription(),

                t.getAmount(),

                t.getType().name(),

                t.getSource().name(),

                t.getCategoryId(),

                t.getBankAccountId(),

                t.getMerchantName(),

                t.getMerchantId(),

                t.getCategorySource() != null
                        ? t.getCategorySource().name()
                        : null,

                t.getCategoryConfidence(),

                t.getCategoryReason(),

                t.isRecurring(),

                t.getNotes(),

                t.getCreatedAt()
        );
    }
}