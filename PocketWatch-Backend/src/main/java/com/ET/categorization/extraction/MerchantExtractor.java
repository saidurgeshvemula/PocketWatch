package com.ET.categorization.extraction;

import com.ET.categorization.dto.ExtractedMerchantInfo;
import com.ET.categorization.dto.PaymentMode;
import com.ET.transaction.entity.Transaction.TransactionType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class MerchantExtractor {

    private static final List<String> KNOWN_BANK_CODES = List.of(
            "HDFC", "SBI", "ICIC", "ICICI", "AXIS", "KOTAK", "PNB", "BOB",
            "IDFC", "YES", "INDUS", "CANARA", "UNION", "FEDERAL", "RBL", "AUBANK"
    );

    private static final Pattern NUMERIC_TOKEN = Pattern.compile("^\\d{4,}$");

    private static final Pattern REFERENCE_NUMBER = Pattern.compile("\\b\\d{6,}\\b");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");
    private static final Pattern TRAILING_PUNCT = Pattern.compile("[./\\-]+$");

    public ExtractedMerchantInfo extract(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return notFound(rawDescription);
        }

        String upper = rawDescription.toUpperCase();

        if (upper.contains("UPI") || upper.contains("IMPS") || upper.contains("BBPS")) {
            PaymentMode mode = upper.contains("UPI") ? PaymentMode.UPI
                    : upper.contains("IMPS") ? PaymentMode.IMPS
                    : PaymentMode.BBPS;
            ExtractedMerchantInfo result = extractSlashDelimited(rawDescription, upper, mode);
            if (result.merchantFound()) return result;
        }
        if (upper.contains("NEFT") || upper.contains("RTGS")) {
            PaymentMode mode = upper.contains("NEFT") ? PaymentMode.NEFT : PaymentMode.RTGS;
            ExtractedMerchantInfo result = extractDashDelimited(rawDescription, mode);
            if (result.merchantFound()) return result;
        }
        if (upper.startsWith("POS") || upper.contains("POS ")) {
            return extractPos(rawDescription);
        }
        if (upper.startsWith("ATM") || upper.contains("ATM ") || upper.contains("ATM-")) {
            return fixedMerchant(rawDescription, "ATM Withdrawal", PaymentMode.ATM);
        }
        if (upper.startsWith("FASTAG")) {
            return fixedMerchant(rawDescription, "FASTag Toll", PaymentMode.FASTAG);
        }
        if (upper.startsWith("NACH") || upper.startsWith("ECS") || upper.startsWith("ACH")) {
            return extractMandate(rawDescription, upper);
        }

        return notFound(rawDescription);
    }

    private ExtractedMerchantInfo extractSlashDelimited(String raw, String upper, PaymentMode mode) {
        String[] tokens = raw.split("/");
        if (tokens.length < 2) return notFound(raw);

        TransactionType direction = detectDirection(tokens);

        String merchant = null;
        String bankCode = null;

        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty()) continue;
            if (isDirectionToken(token) || token.equalsIgnoreCase("P2A")
                    || token.equalsIgnoreCase("P2M") || token.equalsIgnoreCase("P2P")) continue;
            if (NUMERIC_TOKEN.matcher(token).matches()) continue;

            if (bankCode == null) {
                String bank = matchKnownBank(token);
                if (bank != null) {
                    bankCode = bank;
                    continue;
                }
            }
            if (merchant == null) {
                merchant = token;
            }
        }

        if (merchant == null) return notFound(raw);
        return new ExtractedMerchantInfo(raw, clean(raw), clean(merchant), mode, bankCode, direction, true);
    }

    private ExtractedMerchantInfo extractDashDelimited(String raw, PaymentMode mode) {
        String[] tokens = raw.split("-");
        if (tokens.length < 2) return notFound(raw);

        String merchant = null;
        String bankCode = null;
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i].trim();
            if (token.isEmpty() || NUMERIC_TOKEN.matcher(token).matches()) continue;
            if (bankCode == null) {
                String bank = matchKnownBank(token);
                if (bank != null) {
                    bankCode = bank;
                    continue;
                }
            }
            if (merchant == null) {
                merchant = token;
            }
        }
        if (merchant == null) return notFound(raw);
        return new ExtractedMerchantInfo(raw, clean(raw), clean(merchant), mode, bankCode, null, true);
    }

    private ExtractedMerchantInfo extractPos(String raw) {
        String withoutPrefix = raw.replaceFirst("(?i)^POS", "").trim();
        String merchant = clean(withoutPrefix);
        if (merchant.isBlank()) return notFound(raw);
        return new ExtractedMerchantInfo(raw, clean(raw), merchant,
                PaymentMode.POS, null, TransactionType.DEBIT, true);
    }

    private ExtractedMerchantInfo extractMandate(String raw, String upper) {
        PaymentMode mode = upper.startsWith("NACH") ? PaymentMode.NACH
                : upper.startsWith("ECS") ? PaymentMode.ECS
                : PaymentMode.ACH;
        for (String token : raw.split("/")) {
            String t = token.trim();
            if (t.isEmpty() || t.equalsIgnoreCase("NACH") || t.equalsIgnoreCase("ECS")
                    || t.equalsIgnoreCase("ACH") || NUMERIC_TOKEN.matcher(t).matches()) continue;
            return new ExtractedMerchantInfo(raw, clean(raw), clean(t), mode, null, TransactionType.DEBIT, true);
        }
        return notFound(raw);
    }

    private ExtractedMerchantInfo fixedMerchant(String raw, String name, PaymentMode mode) {
        return new ExtractedMerchantInfo(raw, clean(raw), name, mode, null, TransactionType.DEBIT, true);
    }

    private TransactionType detectDirection(String[] tokens) {
        for (String token : tokens) {
            if (token.equalsIgnoreCase("DR")) return TransactionType.DEBIT;
            if (token.equalsIgnoreCase("CR")) return TransactionType.CREDIT;
        }
        return null;
    }

    private boolean isDirectionToken(String token) {
        return token.equalsIgnoreCase("DR") || token.equalsIgnoreCase("CR");
    }

    private String matchKnownBank(String token) {
        String upperToken = token.toUpperCase();
        return KNOWN_BANK_CODES.stream()
                .filter(upperToken::contains)
                .findFirst()
                .orElse(null);
    }

    private ExtractedMerchantInfo notFound(String raw) {
        return new ExtractedMerchantInfo(raw, clean(raw), null, PaymentMode.UNKNOWN, null, null, false);
    }

    private String clean(String raw) {
        if (raw == null) return "";
        String cleaned = raw.trim();
        cleaned = REFERENCE_NUMBER.matcher(cleaned).replaceAll(" ");
        cleaned = MULTI_SPACE.matcher(cleaned).replaceAll(" ");
        cleaned = TRAILING_PUNCT.matcher(cleaned).replaceAll("");
        return cleaned.trim();
    }
}
