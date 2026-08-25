package com.ET.categorization.dto;

public record CategorizationResult(
        Long merchantId,
        Long categoryId,
        String categoryName,
        int confidence,
        CategorizationSource source,
        String reason,
        String merchantName
) {
    public static final int MERCHANT_DB_CONFIDENCE = 99;
    public static final int RULE_ENGINE_CONFIDENCE = 95;
    public static final int USER_OVERRIDE_CONFIDENCE = 100;
}
