package com.ET.analytics.service;

import com.ET.analytics.dto.CategoryBreakdown;
import com.ET.analytics.dto.MerchantSummary;
import com.ET.analytics.dto.MonthlySummary;
import com.ET.analytics.repository.AnalyticsRepository;
import com.ET.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(
            value = "analytics",
            key = "#userId + ':summary:' + #start + ':' + #end + ':' + #bankAccountId"
    )
    public MonthlySummary getSummary(
            Long userId,
            LocalDate start,
            LocalDate end,
            Long bankAccountId
    ) {
        BigDecimal income =
                analyticsRepository.sumIncome(
                        userId,
                        start,
                        end,
                        bankAccountId
                );

        BigDecimal expenses =
                analyticsRepository.sumExpenses(
                        userId,
                        start,
                        end,
                        bankAccountId
                );

        long count =
                analyticsRepository.countTransactions(
                        userId,
                        start,
                        end,
                        bankAccountId
                );

        return new MonthlySummary(
                start.getYear() + "-" +
                        String.format("%02d", start.getMonthValue()),
                income,
                expenses,
                income.subtract(expenses),
                count
        );
    }

    @Cacheable(
            value = "analytics",
            key = "#userId + ':monthly:' + #bankAccountId"
    )
    public List<MonthlySummary> getMonthly(
            Long userId,
            Long bankAccountId
    ) {
        LocalDate since =
                LocalDate.now()
                        .minusMonths(12)
                        .withDayOfMonth(1);

        List<Object[]> rows =
                analyticsRepository.monthlyBreakdown(
                        userId,
                        since,
                        bankAccountId
                );

        return rows.stream()
                .map(r -> {
                    BigDecimal income = (BigDecimal) r[1];
                    BigDecimal expenses = (BigDecimal) r[2];

                    return new MonthlySummary(
                            (String) r[0],
                            income,
                            expenses,
                            income.subtract(expenses),
                            (long) (Long) r[3]
                    );
                })
                .toList();
    }

    @Cacheable(
            value = "analytics",
            key = "#userId + ':categories:' + #start + ':' + #end + ':' + #bankAccountId"
    )
    public List<CategoryBreakdown> getByCategory(
            Long userId,
            LocalDate start,
            LocalDate end,
            Long bankAccountId
    ) {
        List<Object[]> rows =
                analyticsRepository.sumByCategory(
                        userId,
                        start,
                        end,
                        bankAccountId
                );

        Map<Long, String> nameMap =
                categoryRepository.findAll()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        c -> c.getId(),
                                        c -> c.getName()
                                )
                        );

        BigDecimal total =
                rows.stream()
                        .map(r -> (BigDecimal) r[1])
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        return rows.stream()
                .map(r -> {

                    Long catId = (Long) r[0];

                    BigDecimal amount =
                            (BigDecimal) r[1];

                    long count =
                            (Long) r[2];

                    double percentage =
                            total.compareTo(BigDecimal.ZERO) == 0
                                    ? 0
                                    : amount
                                    .divide(
                                            total,
                                            4,
                                            RoundingMode.HALF_UP
                                    )
                                    .multiply(
                                            BigDecimal.valueOf(100)
                                    )
                                    .doubleValue();

                    return new CategoryBreakdown(
                            catId,
                            nameMap.getOrDefault(
                                    catId,
                                    "Unknown"
                            ),
                            amount,
                            count,
                            percentage
                    );
                })
                .toList();
    }

    @Cacheable(
            value = "analytics",
            key = "#userId + ':merchants:' + #start + ':' + #end + ':' + #bankAccountId"
    )
    public List<MerchantSummary> getTopMerchants(
            Long userId,
            LocalDate start,
            LocalDate end,
            Long bankAccountId
    ) {
        return analyticsRepository
                .topMerchants(
                        userId,
                        start,
                        end,
                        bankAccountId
                )
                .stream()
                .limit(10)
                .map(r ->
                        new MerchantSummary(
                                (String) r[0],
                                (BigDecimal) r[1],
                                (Long) r[2]
                        )
                )
                .toList();
    }

    // =========================================================
    // CASH ANALYTICS
    // =========================================================

    public BigDecimal getCashIncome(
            Long userId,
            LocalDate start,
            LocalDate end
    ) {
        return analyticsRepository.sumCashIncome(
                userId,
                start,
                end
        );
    }

    public BigDecimal getCashExpenses(
            Long userId,
            LocalDate start,
            LocalDate end
    ) {
        return analyticsRepository.sumCashExpenses(
                userId,
                start,
                end
        );
    }

    public long getCashTransactionCount(
            Long userId,
            LocalDate start,
            LocalDate end
    ) {
        return analyticsRepository.countCashTransactions(
                userId,
                start,
                end
        );
    }

    @CacheEvict(
            value = "analytics",
            allEntries = true
    )
    public void evictUserCache(Long userId) {
    }
}