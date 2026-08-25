package com.ET.categorization.dto;

import com.ET.transaction.entity.Transaction.TransactionType;

public record ExtractedMerchantInfo(
        String originalDescription,
        String cleanedDescription,
        String merchantRaw,
        PaymentMode paymentMode,
        String bankCode,
        TransactionType direction,
        boolean merchantFound
) {}
