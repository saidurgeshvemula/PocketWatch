package com.ET.analytics.dto;

import java.math.BigDecimal;

public record MerchantSummary(
        String merchantName,
        BigDecimal totalAmount,
        long transactionCount
) {}
