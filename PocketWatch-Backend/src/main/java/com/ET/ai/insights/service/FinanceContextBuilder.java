package com.ET.ai.insights.service;

import com.ET.analytics.dto.CategoryBreakdown;
import com.ET.analytics.dto.MerchantSummary;
import com.ET.analytics.dto.MonthlySummary;
import com.ET.analytics.service.AnalyticsService;
import com.ET.transaction.entity.BankAccount;
import com.ET.transaction.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FinanceContextBuilder {

    private final AnalyticsService analyticsService;
    private final BankAccountRepository bankAccountRepository;

    /**
     * Chat context: NOT tied to the Dashboard's selected month.
     * Gives current month snapshot + 6-month trend so the AI can
     * actually compare/recommend, which buildContext() (single
     * month, single account) cannot support.
     */
    public String buildChatContext(Long userId, Long bankAccountId) {

        YearMonth currentMonth = YearMonth.now();
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        MonthlySummary current =
                analyticsService.getSummary(userId, start, end, bankAccountId);

        List<CategoryBreakdown> cats =
                analyticsService.getByCategory(userId, start, end, bankAccountId);

        List<MerchantSummary> merchants =
                analyticsService.getTopMerchants(userId, start, end, bankAccountId);

        List<MonthlySummary> trend =
                analyticsService.getMonthly(userId, bankAccountId)
                        .stream()
                        .limit(6)
                        .toList();

        BigDecimal cashIncome = analyticsService.getCashIncome(userId, start, end);
        BigDecimal cashExpenses = analyticsService.getCashExpenses(userId, start, end);
        long cashCount = analyticsService.getCashTransactionCount(userId, start, end);

        List<BankAccount> accounts =
                bankAccountRepository.findByUserIdOrderByCreatedAtDesc(userId);

        StringBuilder sb = new StringBuilder();

        sb.append("=== ACCOUNTS ===\n");
        if (bankAccountId == null) {
            sb.append("Scope: ALL ACCOUNTS COMBINED\n");
        } else {
            sb.append("Scope: single account (id ").append(bankAccountId).append(")\n");
        }
        accounts.forEach(a ->
                sb.append("  - [id ").append(a.getId()).append("] ")
                        .append(a.getNickname()).append(" (").append(a.getBankName()).append(")\n")
        );

        sb.append("\n=== CURRENT MONTH (").append(currentMonth).append(") ===\n");
        sb.append("Income: ₹").append(current.totalIncome()).append("\n");
        sb.append("Expenses: ₹").append(current.totalExpenses()).append("\n");
        sb.append("Net Savings: ₹").append(current.netSavings()).append("\n");
        sb.append("Transactions: ").append(current.transactionCount()).append("\n");

        sb.append("\nTop Categories:\n");
        cats.stream().limit(5).forEach(c ->
                sb.append("  - ").append(c.categoryName()).append(": ₹").append(c.totalAmount())
                        .append(" (").append(String.format("%.1f", c.percentage())).append("%)\n")
        );

        sb.append("\nTop Merchants:\n");
        merchants.stream().limit(5).forEach(m ->
                sb.append("  - ").append(m.merchantName()).append(": ₹").append(m.totalAmount()).append("\n")
        );

        sb.append("\n=== CASH (current month, separate from bank accounts) ===\n");
        sb.append("Cash Income: ₹").append(cashIncome).append("\n");
        sb.append("Cash Expenses: ₹").append(cashExpenses).append("\n");
        sb.append("Cash Transactions: ").append(cashCount).append("\n");

        sb.append("\n=== LAST 6 MONTHS TREND (oldest to newest) ===\n");
        trend.forEach(m ->
                sb.append("  - ").append(m.month()).append(": income ₹").append(m.totalIncome())
                        .append(", expenses ₹").append(m.totalExpenses())
                        .append(", net ₹").append(m.netSavings()).append("\n")
        );

        sb.append("\n=== RULES ===\n");
        sb.append("- Use ONLY the data above. Never invent figures.\n");
        sb.append("- Cash and bank-account amounts are separate; never silently combine them.\n");
        sb.append("- Trend data above is monthly totals only (no category/merchant breakdown per past month).\n");
        sb.append("- If asked about something outside this data, say it isn't available.\n");

        return sb.toString();
    }

    public String buildContext(
            Long userId,
            YearMonth selectedMonth,
            Long bankAccountId
    ) {

        LocalDate start = selectedMonth.atDay(1);
        LocalDate end = selectedMonth.atEndOfMonth();

        /*
         * BANK ACCOUNT DATA
         *
         * bankAccountId comes directly from the dashboard.
         */
        MonthlySummary summary =
                analyticsService.getSummary(
                        userId,
                        start,
                        end,
                        bankAccountId
                );

        List<CategoryBreakdown> cats =
                analyticsService.getByCategory(
                        userId,
                        start,
                        end,
                        bankAccountId
                );

        List<MerchantSummary> merchants =
                analyticsService.getTopMerchants(
                        userId,
                        start,
                        end,
                        bankAccountId
                );

        /*
         * CASH DATA
         *
         * Cash is intentionally kept separate from
         * the selected bank account.
         */
        BigDecimal cashIncome =
                analyticsService.getCashIncome(
                        userId,
                        start,
                        end
                );

        BigDecimal cashExpenses =
                analyticsService.getCashExpenses(
                        userId,
                        start,
                        end
                );

        long cashTransactionCount =
                analyticsService.getCashTransactionCount(
                        userId,
                        start,
                        end
                );

        StringBuilder sb = new StringBuilder();

        /*
         * DASHBOARD CONTEXT
         */
        sb.append("=== DASHBOARD FINANCIAL CONTEXT ===\n");

        sb.append("Selected Month: ")
                .append(selectedMonth)
                .append("\n");

        sb.append("Selected Bank Account ID: ")
                .append(bankAccountId == null ? "ALL ACCOUNTS" : bankAccountId)
                .append("\n\n");

        /*
         * SELECTED BANK ACCOUNT
         */
        sb.append("=== SELECTED BANK ACCOUNT DATA ===\n");

        sb.append("Month: ")
                .append(summary.month())
                .append("\n");

        sb.append("Total Income: ₹")
                .append(summary.totalIncome())
                .append("\n");

        sb.append("Total Expenses: ₹")
                .append(summary.totalExpenses())
                .append("\n");

        sb.append("Net Savings: ₹")
                .append(summary.netSavings())
                .append("\n");

        sb.append("Transactions: ")
                .append(summary.transactionCount())
                .append("\n\n");

        /*
         * CATEGORIES
         */
        sb.append("Top Spending Categories:\n");

        cats.stream()
                .limit(5)
                .forEach(c ->
                        sb.append("  - ")
                                .append(c.categoryName())
                                .append(": ₹")
                                .append(c.totalAmount())
                                .append(" (")
                                .append(String.format("%.1f", c.percentage()))
                                .append("%)\n")
                );

        /*
         * MERCHANTS
         */
        sb.append("\nTop Merchants:\n");

        merchants.stream()
                .limit(5)
                .forEach(m ->
                        sb.append("  - ")
                                .append(m.merchantName())
                                .append(": ₹")
                                .append(m.totalAmount())
                                .append("\n")
                );

        /*
         * CASH
         */
        sb.append("\n=== CASH TRANSACTIONS ===\n");

        sb.append("Cash Income: ₹")
                .append(cashIncome)
                .append("\n");

        sb.append("Cash Expenses: ₹")
                .append(cashExpenses)
                .append("\n");

        sb.append("Cash Net: ₹")
                .append(cashIncome.subtract(cashExpenses))
                .append("\n");

        sb.append("Cash Transactions: ")
                .append(cashTransactionCount)
                .append("\n\n");

        /*
         * IMPORTANT RULES FOR THE AI
         */
        sb.append("=== CONTEXT RULES ===\n");

        sb.append("- The selected month is authoritative.\n");

        sb.append("- Bank account data belongs only to the selected bank account.\n");

        sb.append("- Cash transactions are separate from the selected bank account.\n");

        sb.append("- Cash transactions shown above are from the selected month.\n");

        sb.append("- Do not include transactions from other months.\n");

        sb.append("- Do not include transactions from other bank accounts.\n");

        sb.append("- Do not treat cash transactions as bank-account transactions.\n");

        return sb.toString();
    }
}