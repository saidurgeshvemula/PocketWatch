package com.ET.transaction.repository;

import com.ET.transaction.entity.Transaction;
import com.ET.transaction.entity.TransactionSource;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    Optional<Transaction> findTopByUserIdOrderByTransactionDateDesc(Long userId);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.userId = :userId
        AND (:startDate IS NULL OR t.transactionDate >= :startDate)
        AND (:endDate IS NULL OR t.transactionDate <= :endDate)
        AND (:categoryId IS NULL OR t.categoryId = :categoryId)
        AND (:type IS NULL OR t.type = :type)
        AND (:source IS NULL OR t.source = :source)
        AND (:bankAccountId IS NULL OR t.bankAccountId = :bankAccountId)
        AND (:search IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Transaction> findWithFilters(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("categoryId") Long categoryId,
            @Param("type") Transaction.TransactionType type,
            @Param("source") TransactionSource source,
            @Param("bankAccountId") Long bankAccountId,
            @Param("search") String search,
            Pageable pageable
    );

    long countByUserIdAndDescriptionIgnoreCaseAndType(
            Long userId,
            String description,
            Transaction.TransactionType type
    );

    long countByUserIdAndTransactionDateAndDescriptionAndAmountAndType(
            Long userId,
            LocalDate transactionDate,
            String description,
            BigDecimal amount,
            Transaction.TransactionType type
    );

    List<Transaction> findAllByIdInAndUserId(
            List<Long> ids,
            Long userId
    );

    @Modifying
    @Transactional
    @Query("""
            DELETE FROM Transaction t
            WHERE t.userId = :userId
            AND t.id IN :ids
            """)
    int deleteSelected(
            @Param("userId") Long userId,
            @Param("ids") List<Long> ids
    );

    @Modifying
    @Transactional
    @Query("""
            DELETE FROM Transaction t
            WHERE t.userId = :userId
            AND t.transactionDate BETWEEN :startDate AND :endDate
            """)
    int deleteByDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}