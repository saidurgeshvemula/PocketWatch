package com.ET.ai.insights.service;

import com.ET.ai.client.OpenAIClient;
import com.ET.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIInsightsService {

    private final OpenAIClient openAIClient;
    private final FinanceContextBuilder contextBuilder;

    private static final String SYSTEM_PROMPT = """
            You are a personal finance advisor.

            The financial context comes from the user's dashboard selection.

            IMPORTANT CONTEXT RULES:

            1. The selected month is authoritative.
            2. The selected bank account is authoritative.
            3. Analyze only transactions from the selected bank account
               during the selected month.
            4. Cash transactions from the selected month must also be
               considered, but cash is a separate entity.
            5. Do NOT treat cash transactions as transactions belonging
               to the selected bank account.
            6. Do NOT include transactions from other bank accounts.
            7. Do NOT include transactions from other months.
            8. When discussing expenses, distinguish bank-account
               expenses from cash expenses.
            9. If you mention a combined amount, clearly state that it
               includes both bank-account and cash transactions.
            10. Never invent financial information.

            Analyze the financial data and return EXACTLY 5 actionable insights,
            with each insight covering a DIFFERENT one of these areas
            (skip an area only if the data has nothing relevant to say):

            1. Income
            2. Expenses
            3. Savings
            4. Categories
            5. Merchants
            6. Cash spending

            Pick the 5 most relevant of these 6 areas for this month's data.

            Rules:
            - Output only a numbered list (1-5).
            - No introduction or conclusion.
            - Each insight: 2 short sentences.
            - Include specific numbers when possible.
            - End each insight with one recommendation.
            - Be concise, encouraging, and honest.
            """;

    @Cacheable(
            value = "insights",
            key = "#userId + ':' + #selectedMonth + ':' + #bankAccountId"
    )
    public String generateInsights(
            Long userId,
            YearMonth selectedMonth,
            Long bankAccountId
    ) {

        log.info(
                "Generating AI insights for userId={}, month={}, bankAccountId={}",
                userId,
                selectedMonth,
                bankAccountId
        );

        String financialContext =
                contextBuilder.buildContext(
                        userId,
                        selectedMonth,
                        bankAccountId
                );

        String userMessage =
                financialContext
                        + """



                Provide exactly 5 actionable spending insights based
                only on this selected dashboard data.

                Remember to consider the cash transactions separately.
                """;

        List<ChatMessage> messages = List.of(
                ChatMessage.system(SYSTEM_PROMPT),
                ChatMessage.user(userMessage)
        );

        return openAIClient.chat(messages, 800);
    }
}