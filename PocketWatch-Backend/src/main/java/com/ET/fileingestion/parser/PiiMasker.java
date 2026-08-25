package com.ET.fileingestion.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class PiiMasker {

    private static final Pattern IFSC =
            Pattern.compile("\\b[A-Z]{4}0[A-Z0-9]{6}\\b");

    private static final Pattern PAN =
            Pattern.compile("\\b[A-Z]{5}[0-9]{4}[A-Z]\\b");

    private static final Pattern AADHAAR =
            Pattern.compile("\\b(?:\\d{4}\\s?){2}\\d{4}\\b");

    private static final Pattern PHONE =
            Pattern.compile("\\b(?:\\+91[-\\s]?)?[6-9]\\d{9}\\b");

    private static final Pattern EMAIL =
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    private static final Pattern UPI =
            Pattern.compile("\\b[A-Za-z0-9._-]+@[A-Za-z]{2,20}\\b");

    private static final Pattern LONG_NUMBER =
            Pattern.compile("\\b\\d{9,18}\\b");

    private static final Pattern MASKED_ACCOUNT =
            Pattern.compile("\\b(?:X{2,}|\\*{2,})\\d{4}\\b", Pattern.CASE_INSENSITIVE);

    public String mask(String text) {

        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = text;

        masked = IFSC.matcher(masked).replaceAll("[IFSC]");
        masked = PAN.matcher(masked).replaceAll("[PAN]");
        masked = AADHAAR.matcher(masked).replaceAll("[AADHAAR]");
        masked = PHONE.matcher(masked).replaceAll("[PHONE]");
        masked = EMAIL.matcher(masked).replaceAll("[EMAIL]");
        masked = UPI.matcher(masked).replaceAll("[UPI]");
        masked = MASKED_ACCOUNT.matcher(masked).replaceAll("[ACCOUNT]");
        masked = LONG_NUMBER.matcher(masked).replaceAll("[ID]");

        if (!text.equals(masked)) {
            log.info("PII detected and masked before AI processing.");
        } else {
            log.info("No PII detected in extracted statement.");
        }

        return masked;
    }
}