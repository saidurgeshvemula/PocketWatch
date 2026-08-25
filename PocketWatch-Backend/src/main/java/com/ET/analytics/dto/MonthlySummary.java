package com.ET.analytics.dto;

import java.math.BigDecimal;

public record MonthlySummary(
        String month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netSavings,
        long transactionCount
) {}
