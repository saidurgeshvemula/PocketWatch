package com.ET.fileingestion.parser;

import com.ET.fileingestion.dto.ParsedTransaction;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CsvParser {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd MMM yyyy")
    );

    public List<ParsedTransaction> parse(InputStream inputStream) throws IOException {
        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        List<CSVRecord> records = readRecords(content);
        if (records.isEmpty()) return List.of();

        BankFormat format = detectFormat(records.get(0));
        log.info("Detected bank format: {}", format);

        List<ParsedTransaction> transactions = new ArrayList<>();
        for (CSVRecord record : records) {
            try {
                ParsedTransaction tx = mapRecord(record, format);
                if (tx != null) {
                    transactions.add(tx);
                }
            } catch (Exception e) {

                log.warn("Skipping row {}: {}", record.getRecordNumber(), e.getMessage());
            }
        }

        log.info("Parsed {} transactions from CSV", transactions.size());
        return transactions;
    }

    private List<CSVRecord> readRecords(String content) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)

                .setIgnoreHeaderCase(true)
                .build();

        String tableCsv = skipToHeaderRow(content);

        try (CSVParser parser = new CSVParser(new java.io.StringReader(tableCsv), format)) {
            return parser.getRecords();
        }
    }

    private String skipToHeaderRow(String content) {
        String[] lines = content.split("\r\n|\n|\r");
        for (int i = 0; i < lines.length; i++) {
            String lower = lines[i].toLowerCase();
            boolean hasDateCol = lower.contains("date");
            boolean hasDescCol = lower.contains("description") || lower.contains("narration")
                    || lower.contains("particulars") || lower.contains("remarks")
                    || lower.contains("details");
            boolean hasAmountCol = lower.contains("debit") || lower.contains("credit")
                    || lower.contains("amount") || lower.contains("withdrawal")
                    || lower.contains("deposit") || lower.contains("cr/dr");

            if (hasDateCol && hasDescCol && hasAmountCol) {
                if (i > 0) {
                    log.info("Skipped {} metadata row(s) before transaction table header", i);
                }
                return String.join("\n", java.util.Arrays.copyOfRange(lines, i, lines.length));
            }
        }

        return content;
    }

    private BankFormat detectFormat(CSVRecord firstRecord) {

        Map<String, Integer> headers = firstRecord.getParser().getHeaderMap();
        String headerStr = String.join(",", headers.keySet()).toLowerCase();

        if (headerStr.contains("narration") && headerStr.contains("withdrawal")) {
            return BankFormat.HDFC;
        } else if (headerStr.contains("txn date") && headerStr.contains("debit") && headerStr.contains("credit")) {

            return BankFormat.SBI;
        } else if (headerStr.contains("transaction remarks") || headerStr.contains("cr/dr")) {
            return BankFormat.ICICI;
        } else if (headerStr.contains("particulars") && headerStr.contains("bal")) {
            return BankFormat.AXIS;
        } else if (headerStr.contains("value date") && headerStr.contains("details")
                && headerStr.contains("debit") && headerStr.contains("credit")) {
            return BankFormat.VALUE_DATE;
        }
        return BankFormat.GENERIC;
    }

    private ParsedTransaction mapRecord(CSVRecord record, BankFormat format) {
        return switch (format) {
            case HDFC -> mapHdfc(record);
            case SBI -> mapSbi(record);
            case ICICI -> mapIcici(record);
            case AXIS -> mapAxis(record);
            case VALUE_DATE -> mapValueDate(record);
            case GENERIC -> mapGeneric(record);
        };
    }

    private ParsedTransaction mapHdfc(CSVRecord record) {
        String dateStr = getColumn(record, "Date");
        String description = getColumn(record, "Narration");
        String withdrawal = getColumn(record, "Withdrawal Amt.");
        String deposit = getColumn(record, "Deposit Amt.");

        if (dateStr == null || description == null) return null;

        boolean isDebit = withdrawal != null && !withdrawal.isBlank();
        String rawAmount = isDebit ? withdrawal : deposit;
        if (rawAmount == null || rawAmount.isBlank()) return null;

        return ParsedTransaction.builder()
                .transactionDate(parseDate(dateStr))
                .description(description.trim())
                .amount(parseAmount(rawAmount))
                .rawType(isDebit ? "DR" : "CR")
                .rawAmount(rawAmount)
                .build();
    }

    private ParsedTransaction mapSbi(CSVRecord record) {
        String dateStr = getColumn(record, "Txn Date");
        String description = getColumn(record, "Description");
        String debit = getColumn(record, "Debit");
        String credit = getColumn(record, "Credit");

        if (dateStr == null || description == null) return null;

        boolean isDebit = debit != null && !debit.isBlank();
        String rawAmount = isDebit ? debit : credit;
        if (rawAmount == null || rawAmount.isBlank()) return null;

        return ParsedTransaction.builder()
                .transactionDate(parseDate(dateStr))
                .description(description.trim())
                .amount(parseAmount(rawAmount))
                .rawType(isDebit ? "DR" : "CR")
                .rawAmount(rawAmount)
                .build();
    }

    private ParsedTransaction mapIcici(CSVRecord record) {
        String dateStr = getColumn(record, "Transaction Date");
        String description = getColumn(record, "Transaction Remarks");
        String amount = getColumn(record, "Amount(INR)");
        String type = getColumn(record, "CR/DR");

        if (dateStr == null || description == null || amount == null) return null;

        return ParsedTransaction.builder()
                .transactionDate(parseDate(dateStr))
                .description(description.trim())
                .amount(parseAmount(amount))
                .rawType(type)
                .rawAmount(amount)
                .build();
    }

    private ParsedTransaction mapAxis(CSVRecord record) {
        String dateStr = getColumn(record, "Tran Date");
        String description = getColumn(record, "PARTICULARS");
        String dr = getColumn(record, "DR");
        String cr = getColumn(record, "CR");

        if (dateStr == null || description == null) return null;

        boolean isDebit = dr != null && !dr.isBlank();
        String rawAmount = isDebit ? dr : cr;
        if (rawAmount == null || rawAmount.isBlank()) return null;

        return ParsedTransaction.builder()
                .transactionDate(parseDate(dateStr))
                .description(description.trim())
                .amount(parseAmount(rawAmount))
                .rawType(isDebit ? "DR" : "CR")
                .rawAmount(rawAmount)
                .build();
    }

    private ParsedTransaction mapValueDate(CSVRecord record) {
        String dateStr = getColumn(record, "Value Date");
        if (dateStr == null) dateStr = getColumn(record, "Post Date");
        String description = getColumn(record, "Details");
        String debit = getColumn(record, "Debit");
        String credit = getColumn(record, "Credit");

        if (dateStr == null || description == null) return null;

        boolean isDebit = debit != null && !debit.isBlank();
        String rawAmount = isDebit ? debit : credit;
        if (rawAmount == null || rawAmount.isBlank()) return null;

        return ParsedTransaction.builder()
                .transactionDate(parseDate(dateStr))

                .description(description.replaceAll("\\s+", " ").trim())
                .amount(parseAmount(rawAmount))
                .rawType(isDebit ? "DR" : "CR")
                .rawAmount(rawAmount)
                .build();
    }

    private ParsedTransaction mapGeneric(CSVRecord record) {
        String dateStr = getColumn(record, "date");
        String description = getColumn(record, "description");
        if (dateStr == null || description == null) return null;

        String amount = getColumn(record, "amount");
        if (amount != null && !amount.isBlank()) {
            String type = getColumn(record, "type");
            return ParsedTransaction.builder()
                    .transactionDate(parseDate(dateStr))
                    .description(description.trim())
                    .amount(parseAmount(amount))
                    .rawType(type != null ? type : "DR")
                    .rawAmount(amount)
                    .build();
        }

        String debit = getColumn(record, "debit");
        String credit = getColumn(record, "credit");
        if (debit == null && credit == null) return null;

        boolean isDebit = debit != null && !debit.isBlank();
        String rawAmount = isDebit ? debit : credit;
        if (rawAmount == null || rawAmount.isBlank()) return null;

        return ParsedTransaction.builder()
                .transactionDate(parseDate(dateStr))
                .description(description.trim())
                .amount(parseAmount(rawAmount))
                .rawType(isDebit ? "DR" : "CR")
                .rawAmount(rawAmount)
                .build();
    }

    private String getColumn(CSVRecord record, String columnName) {
        try {
            String value = record.get(columnName);
            return value != null && !value.isBlank() ? value.trim() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return null;
        String cleaned = dateStr.trim();
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException("Cannot parse date: " + dateStr);
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) return BigDecimal.ZERO;

        String cleaned = amountStr.replaceAll("[,₹$€£\\s]", "").trim();
        if (cleaned.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(cleaned).abs();
    }
}