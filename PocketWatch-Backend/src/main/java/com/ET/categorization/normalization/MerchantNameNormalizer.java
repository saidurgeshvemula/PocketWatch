package com.ET.categorization.normalization;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
public class MerchantNameNormalizer {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

    public String normalize(String rawMerchant) {
        if (rawMerchant == null || rawMerchant.isBlank()) return "";

        String value = Normalizer.normalize(rawMerchant, Normalizer.Form.NFD);
        value = DIACRITICS.matcher(value).replaceAll("");
        value = value.toUpperCase();
        value = NON_ALPHANUMERIC.matcher(value).replaceAll("");
        return value;
    }
}
