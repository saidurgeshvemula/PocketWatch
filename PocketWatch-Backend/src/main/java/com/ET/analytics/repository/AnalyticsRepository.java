package com.ET.analytics.repository;

import com.ET.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnalyticsRepository
        extends JpaRepository<Transaction, Long> {

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.userId = :uid
          AND t.type = 'CREDIT'
          AND t.transactionDate BETWEEN :start AND :end
          AND (
              :bankAccountId IS NULL
              OR t.bankAccountId = :bankAccountId
          )
        """)
    BigDecimal sumIncome(
            @Param("uid") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("bankAccountId") Long bankAccountId
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.userId = :uid
          AND t.type = 'DEBIT'
          AND t.transactionDate BETWEEN :start AND :end
          AND (
              :bankAccountId IS NULL
              OR t.bankAccountId = :bankAccountId
          )
        """)
    BigDecimal sumExpenses(
            @Param("uid") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("bankAccountId") Long bankAccountId
    );

    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.userId = :uid
          AND t.transactionDate BETWEEN :start AND :end
          AND (
              :bankAccountId IS NULL
              OR t.bankAccountId = :bankAccountId
          )
        """)
    long countTransactions(
            @Param("uid") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("bankAccountId") Long bankAccountId
    );

    @Query("""
        SELECT t.categoryId,
               COALESCE(SUM(t.amount), 0),
               COUNT(t)
        FROM Transaction t
        WHERE t.userId = :uid
          AND t.type = 'DEBIT'
          AND t.transactionDate BETWEEN :start AND :end
          AND t.categoryId IS NOT NULL
          AND (
              :bankAccountId IS NULL
              OR t.bankAccountId = :bankAccountId
          )
        GROUP BY t.categoryId
        ORDER BY SUM(t.amount) DESC
        """)
    List<Object[]> sumByCategory(
            @Param("uid") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("bankAccountId") Long bankAccountId
    );

    @Query("""
        SELECT t.merchantName,
               COALESCE(SUM(t.amount), 0),
               COUNT(t)
        FROM Transaction t
        WHERE t.userId = :uid
          AND t.type = 'DEBIT'
          AND t.merchantName IS NOT NULL
          AND t.transactionDate BETWEEN :start AND :end
          AND (
              :bankAccountId IS NULL
              OR t.bankAccountId = :bankAccountId
          )
        GROUP BY t.merchantName
        ORDER BY SUM(t.amount) DESC
        """)
    List<Object[]> topMerchants(
            @Param("uid") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("bankAccountId") Long bankAccountId
    );

    @Query("""
        SELECT FUNCTION('DATE_FORMAT', t.transactionDate, '%Y-%m'),
               COALESCE(
                   SUM(
                       CASE
                           WHEN t.type = 'CREDIT'
                           THEN t.amount
                           ELSE 0
                       END
                   ), 0
               ),
               COALESCE(
                   SUM(
                       CASE
                           WHEN t.type = 'DEBIT'
                           THEN t.amount
                           ELSE 0
                       END
                   ), 0
               ),
               COUNT(t)
        FROM Transaction t
        WHERE t.userId = :uid
          AND t.transactionDate >= :since
          AND (
              :bankAccountId IS NULL
              OR t.bankAccountId = :bankAccountId
          )
        GROUP BY FUNCTION(
            'DATE_FORMAT',
            t.transactionDate,
            '%Y-%m'
        )
        ORDER BY 1 ASC
        """)
    List<Object[]> monthlyBreakdown(
            @Param("uid") Long userId,
            @Param("since") LocalDate since,
            @Param("bankAccountId") Long bankAccountId
    );

    // =========================================================
    // CASH TRANSACTIONS
    // =========================================================

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.userId = :uid
          AND t.source =
              com.ET.transaction.entity.TransactionSource.CASH
          AND t.type = 'CREDIT'
          AND t.transactionDate BETWEEN :start AND :end
        """)
    BigDecimal sumCashIncome(
            @Param("uid") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.userId = :uid
          AND t.source =
              com.ET.transaction.entity.TransactionSource.CASH
          AND t.type = 'DEBIT'
          AND t.transactionDate BETWEEN :start AND :end
        """)
    BigDecimal sumCashExpenses(
            @Param("uid") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.userId = :uid
          AND t.source =
              com.ET.transaction.entity.TransactionSource.CASH
          AND t.transactionDate BETWEEN :start AND :end
        """)
    long countCashTransactions(
            @Param("uid") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );
}