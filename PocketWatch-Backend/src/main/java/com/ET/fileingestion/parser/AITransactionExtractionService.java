package com.ET.fileingestion.parser;

import com.ET.ai.client.OpenAIClient;
import com.ET.ai.dto.ChatMessage;
import com.ET.fileingestion.dto.ParsedTransaction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AITransactionExtractionService {

    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;


    /*
     * ============================================================
     * CHUNK CONFIGURATION
     * ============================================================
     */

    /*
     * Smaller than the previous 1500 chars.
     *
     * This reduces the amount of reasoning/output required
     * from GPT-OSS.
     */
    private static final int CHUNK_CHARS = 1200;

    /*
     * Small overlap so transactions around a boundary aren't lost.
     */
    private static final int CHUNK_OVERLAP_CHARS = 200;


    /*
     * ============================================================
     * RETRY CONFIGURATION
     * ============================================================
     */

    /*
     * Only one retry at extraction level.
     *
     * OpenAIClient itself also has only one 429 retry.
     *
     * Therefore we don't create a retry explosion.
     */
    private static final int MAX_RETRIES = 1;

    private static final long INITIAL_RETRY_DELAY_MS = 10000L;


    /*
     * ============================================================
     * SYSTEM PROMPT
     * ============================================================
     */

    private static final String SYSTEM_PROMPT = """
            You extract transactions from bank statement text.

            The text may be messy:
            - OCR artifacts
            - wrapped lines
            - merged columns
            - masked IDs like [PHONE] or [ID]

            Ignore masked IDs and other non-transaction data.

            Return ONLY a valid JSON array.
            No markdown fences.
            No explanation.

            Each item must have exactly this structure:

            {
              "date": "YYYY-MM-DD",
              "description": "...",
              "amount": 123.45,
              "type": "DEBIT" | "CREDIT"
            }

            DATE FORMAT — IMPORTANT:

            These are Indian bank statements.

            Every date in the source text is day-first:

            DD/MM/YYYY
            DD-MM-YYYY
            DD Mon YYYY

            Example:

            05/06/2025 means 5 June 2025,
            NOT May 6.

            Never interpret dates using US-style MM/DD/YYYY.

            Always treat the first number as the day
            and the second number as the month.

            Convert the date to YYYY-MM-DD.

            Rules:

            - amount must always be a positive number.
            - type must be DEBIT for withdrawals/debits.
            - type must be CREDIT for deposits/credits.
            - Skip header rows.
            - Skip opening balances.
            - Skip closing balances.
            - Skip page footers.
            - Skip summary rows.
            - Extract only actual transaction lines.
            - If a transaction's date cannot be determined, skip it.
            - If a transaction's amount cannot be determined, skip it.
            - If this chunk contains no transactions, return [].
            """;


    /*
     * ============================================================
     * PUBLIC EXTRACTION
     * ============================================================
     */

    public List<ParsedTransaction> extract(
            String rawText
    ) {

        if (rawText == null
                || rawText.isBlank()) {

            log.warn(
                    "AI transaction extraction received empty text"
            );

            return List.of();
        }


        List<String> chunks =
                chunk(rawText);


        if (chunks.isEmpty()) {

            return List.of();
        }


        log.info(
                "Starting AI transaction extraction: "
                        + "{} characters split into {} chunks",
                rawText.length(),
                chunks.size()
        );


        List<ParsedTransaction> all =
                new ArrayList<>();


        /*
         * All extracted transaction keys.
         *
         * Using a global set is safer than only comparing
         * with the immediately previous chunk.
         */
        Set<String> seenTransactions =
                new HashSet<>();


        for (int i = 0;
             i < chunks.size();
             i++) {

            String chunkText =
                    chunks.get(i);


            int chunkNumber =
                    i + 1;


            log.info(
                    "Processing extraction chunk {}/{} ({} characters)",
                    chunkNumber,
                    chunks.size(),
                    chunkText.length()
            );


            List<ParsedTransaction> chunkResults;


            try {

                chunkResults =
                        extractChunkWithRetry(
                                chunkText,
                                chunkNumber,
                                chunks.size()
                        );


            } catch (Exception e) {

                /*
                 * IMPORTANT:
                 *
                 * Do NOT recursively split.
                 *
                 * Do NOT keep retrying for minutes.
                 *
                 * Fail the upload cleanly so the user can retry.
                 */
                log.error(
                        "Extraction failed for chunk {}/{}: {}",
                        chunkNumber,
                        chunks.size(),
                        e.getMessage(),
                        e
                );


                throw new RuntimeException(
                        "AI transaction extraction failed "
                                + "on chunk "
                                + chunkNumber
                                + "/"
                                + chunks.size()
                                + ": "
                                + e.getMessage(),
                        e
                );
            }


            int added =
                    0;


            for (ParsedTransaction tx :
                    chunkResults) {

                String key =
                        transactionKey(tx);


                if (seenTransactions.add(key)) {

                    all.add(tx);

                    added++;

                } else {

                    log.debug(
                            "Skipping duplicate transaction: {}",
                            tx.getDescription()
                    );
                }
            }


            log.info(
                    "Chunk {}/{} completed: "
                            + "{} extracted, {} new",
                    chunkNumber,
                    chunks.size(),
                    chunkResults.size(),
                    added
            );
        }


        log.info(
                "AI transaction extraction completed: "
                        + "{} total unique transactions",
                all.size()
        );


        return all;
    }


    /*
     * ============================================================
     * EXTRACTION + SINGLE RETRY
     * ============================================================
     */

    private List<ParsedTransaction> extractChunkWithRetry(
            String chunk,
            int chunkNumber,
            int totalChunks
    ) {

        Exception lastException = null;


        for (int attempt = 0;
             attempt <= MAX_RETRIES;
             attempt++) {

            try {

                log.info(
                        "AI extraction request for chunk {}/{} "
                                + "- attempt {}/{}",
                        chunkNumber,
                        totalChunks,
                        attempt + 1,
                        MAX_RETRIES + 1
                );


                List<ParsedTransaction> result =
                        extractChunk(chunk);


                log.info(
                        "AI extraction succeeded for chunk {}/{}: "
                                + "{} transactions",
                        chunkNumber,
                        totalChunks,
                        result.size()
                );


                return result;


            } catch (
                    OpenAIClient.AIResponseTooLongException e
            ) {

                /*
                 * Don't recursively split.
                 *
                 * If 1200 output tokens isn't enough even with
                 * low reasoning, the chunk should fail rather than
                 * explode into many requests.
                 */
                log.error(
                        "AI response was truncated for chunk {}/{}. "
                                + "Not recursively splitting.",
                        chunkNumber,
                        totalChunks
                );


                throw e;


            } catch (Exception e) {

                lastException =
                        e;


                if (!isRetryableException(e)
                        || attempt >= MAX_RETRIES) {

                    throw e;
                }


                long delay =
                        calculateRetryDelay(
                                e,
                                attempt
                        );


                log.warn(
                        "Retryable AI error on chunk {}/{}. "
                                + "Retrying once in {} ms: {}",
                        chunkNumber,
                        totalChunks,
                        delay,
                        e.getMessage()
                );


                sleep(delay);
            }
        }


        throw new RuntimeException(
                "AI extraction failed",
                lastException
        );
    }


    /*
     * ============================================================
     * ACTUAL AI REQUEST
     * ============================================================
     */

    private List<ParsedTransaction> extractChunk(
            String chunk
    ) {

        List<ChatMessage> messages =
                List.of(
                        ChatMessage.system(
                                SYSTEM_PROMPT
                        ),
                        ChatMessage.user(
                                chunk
                        )
                );


        /*
         * IMPORTANT:
         *
         * 1200 instead of the old 2500.
         *
         * GPT-OSS reasoning is also explicitly set to LOW
         * inside OpenAIClient.
         */
        String response =
                openAIClient.chat(
                        messages,
                        1200
                );


        return parseResponse(response);
    }


    /*
     * ============================================================
     * RETRYABLE ERROR DETECTION
     * ============================================================
     */

    private boolean isRetryableException(
            Exception e
    ) {

        String message =
                buildFullExceptionMessage(e)
                        .toLowerCase();


        return message.contains("429")
                || message.contains("too many requests")
                || message.contains("rate limit")
                || message.contains("rate_limit_exceeded")
                || message.contains("tokens per minute")
                || message.contains("tpm")
                || message.contains("timeout")
                || message.contains("timed out")
                || message.contains("connection reset")
                || message.contains("connection refused")
                || message.contains("503")
                || message.contains("502")
                || message.contains("504")
                || message.contains("service unavailable")
                || message.contains("temporarily unavailable");
    }


    /*
     * ============================================================
     * EXCEPTION MESSAGE
     * ============================================================
     */

    private String buildFullExceptionMessage(
            Throwable throwable
    ) {

        StringBuilder message =
                new StringBuilder();


        Throwable current =
                throwable;


        while (current != null) {

            if (current.getMessage() != null) {

                message.append(
                        current.getMessage()
                ).append(" ");
            }


            current =
                    current.getCause();
        }


        return message.toString();
    }


    /*
     * ============================================================
     * RETRY DELAY
     * ============================================================
     */

    private long calculateRetryDelay(
            Exception e,
            int attempt
    ) {

        String message =
                buildFullExceptionMessage(e);


        Pattern pattern =
                Pattern.compile(
                        "try again in\\s+([0-9.]+)s",
                        Pattern.CASE_INSENSITIVE
                );


        Matcher matcher =
                pattern.matcher(message);


        if (matcher.find()) {

            try {

                double seconds =
                        Double.parseDouble(
                                matcher.group(1)
                        );


                return (long)
                        (seconds * 1000L)
                        + 2000L;

            } catch (NumberFormatException ignored) {

                // Use fallback.
            }
        }


        return INITIAL_RETRY_DELAY_MS;
    }


    /*
     * ============================================================
     * RESPONSE PARSING
     * ============================================================
     */

    private List<ParsedTransaction> parseResponse(
            String response
    ) {

        List<RawItem> items =
                tryParseArray(response);


        if (items == null) {

            items =
                    tryParseArray(
                            extractArraySubstring(
                                    response
                            )
                    );
        }


        if (items == null) {

            items =
                    salvageObjects(response);
        }


        if (items == null) {

            throw new RuntimeException(
                    "AI returned invalid transaction JSON"
            );
        }


        List<ParsedTransaction> result =
                new ArrayList<>();


        for (RawItem item :
                items) {

            try {

                if (item.date == null
                        || item.date.isBlank()
                        || item.description == null
                        || item.description.isBlank()
                        || item.amount == null
                        || item.type == null
                        || item.type.isBlank()) {

                    log.warn(
                            "Skipping incomplete AI transaction: {}",
                            item
                    );

                    continue;
                }


                result.add(
                        ParsedTransaction.builder()
                                .transactionDate(
                                        LocalDate.parse(
                                                item.date.trim()
                                        )
                                )
                                .description(
                                        item.description.trim()
                                )
                                .amount(
                                        new BigDecimal(
                                                String.valueOf(
                                                        item.amount
                                                )
                                        ).abs()
                                )
                                .rawType(
                                        item.type
                                                .trim()
                                                .toUpperCase()
                                )
                                .rawAmount(
                                        String.valueOf(
                                                item.amount
                                        )
                                )
                                .build()
                );


            } catch (Exception e) {

                log.warn(
                        "Skipping malformed AI transaction: {}",
                        item,
                        e
                );
            }
        }


        return result;
    }


    /*
     * ============================================================
     * JSON ARRAY PARSER
     * ============================================================
     */

    private List<RawItem> tryParseArray(
            String text
    ) {

        if (text == null
                || text.isBlank()) {

            return null;
        }


        try {

            String cleaned =
                    text
                            .replace(
                                    "```json",
                                    ""
                            )
                            .replace(
                                    "```",
                                    ""
                            )
                            .trim();


            return objectMapper.readValue(
                    cleaned,
                    new TypeReference<List<RawItem>>() {
                    }
            );


        } catch (Exception e) {

            return null;
        }
    }


    /*
     * ============================================================
     * EXTRACT JSON ARRAY
     * ============================================================
     */

    private String extractArraySubstring(
            String response
    ) {

        if (response == null) {

            return null;
        }


        int start =
                response.indexOf('[');


        int end =
                response.lastIndexOf(']');


        if (start < 0
                || end < start) {

            return null;
        }


        return response.substring(
                start,
                end + 1
        );
    }


    /*
     * ============================================================
     * SALVAGE JSON OBJECTS
     * ============================================================
     */

    private List<RawItem> salvageObjects(
            String response
    ) {

        if (response == null
                || response.isBlank()) {

            return null;
        }


        List<RawItem> result =
                new ArrayList<>();


        Matcher matcher =
                Pattern
                        .compile(
                                "\\{[^{}]*\\}"
                        )
                        .matcher(response);


        while (matcher.find()) {

            try {

                RawItem item =
                        objectMapper.readValue(
                                matcher.group(),
                                RawItem.class
                        );


                result.add(item);

            } catch (Exception ignored) {

                // Ignore malformed object.
            }
        }


        return result.isEmpty()
                ? null
                : result;
    }


    /*
     * ============================================================
     * CHUNKING
     * ============================================================
     */

    private List<String> chunk(
            String text
    ) {

        if (text == null
                || text.isBlank()) {

            return List.of();
        }


        String[] lines =
                text.split("\\R");


        List<String> chunks =
                new ArrayList<>();


        StringBuilder current =
                new StringBuilder();


        for (String line :
                lines) {

            /*
             * Handle an unusually long line.
             */
            if (line.length()
                    > CHUNK_CHARS
                    && current.isEmpty()) {

                chunks.addAll(
                        splitLongLine(line)
                );

                continue;
            }


            if (current.length()
                    + line.length()
                    + 1
                    > CHUNK_CHARS
                    && !current.isEmpty()) {

                String completed =
                        current.toString();


                chunks.add(
                        completed
                );


                /*
                 * Keep only a small tail for boundary context.
                 */
                current =
                        new StringBuilder(
                                tail(
                                        completed,
                                        CHUNK_OVERLAP_CHARS
                                )
                        );
            }


            current
                    .append(line)
                    .append('\n');
        }


        if (!current.isEmpty()) {

            chunks.add(
                    current.toString()
            );
        }


        return chunks;
    }


    /*
     * ============================================================
     * LONG LINE
     * ============================================================
     */

    private List<String> splitLongLine(
            String line
    ) {

        List<String> result =
                new ArrayList<>();


        int start =
                0;


        while (start < line.length()) {

            int end =
                    Math.min(
                            start + CHUNK_CHARS,
                            line.length()
                    );


            result.add(
                    line.substring(
                            start,
                            end
                    )
            );


            start =
                    end;
        }


        return result;
    }


    /*
     * ============================================================
     * DUPLICATE KEY
     * ============================================================
     */

    private String transactionKey(
            ParsedTransaction tx
    ) {

        return String.valueOf(
                tx.getTransactionDate()
        )
                + "|"
                + normalizeDescription(
                tx.getDescription()
        )
                + "|"
                + String.valueOf(
                tx.getAmount()
        )
                + "|"
                + String.valueOf(
                tx.getRawType()
        );
    }


    private String normalizeDescription(
            String description
    ) {

        if (description == null) {

            return "";
        }


        return description
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toLowerCase();
    }


    /*
     * ============================================================
     * TAIL
     * ============================================================
     */

    private String tail(
            String text,
            int maxChars
    ) {

        if (text == null
                || text.isEmpty()) {

            return "";
        }


        if (text.length()
                <= maxChars) {

            return text;
        }


        String result =
                text.substring(
                        text.length()
                                - maxChars
                );


        int newline =
                result.indexOf('\n');


        if (newline >= 0) {

            return result.substring(
                    newline + 1
            );
        }


        return result;
    }


    /*
     * ============================================================
     * SLEEP
     * ============================================================
     */

    private void sleep(
            long milliseconds
    ) {

        try {

            Thread.sleep(
                    milliseconds
            );

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();


            throw new RuntimeException(
                    "AI extraction retry interrupted",
                    e
            );
        }
    }


    /*
     * ============================================================
     * RAW AI ITEM
     * ============================================================
     */

    private static class RawItem {

        public String date;

        public String description;

        public Object amount;

        public String type;


        @Override
        public String toString() {

            return "RawItem{" +
                    "date='" + date + '\'' +
                    ", description='" + description + '\'' +
                    ", amount=" + amount +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}