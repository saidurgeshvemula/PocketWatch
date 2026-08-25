package com.ET.analytics.dto;

import java.math.BigDecimal;

public record CategoryBreakdown(
        Long categoryId,
        String categoryName,
        BigDecimal totalAmount,
        long transactionCount,
        double percentage
) {}
