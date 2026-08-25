package com.ET.transaction.service;

import com.ET.categorization.dto.CategorizationSource;
import com.ET.categorization.merchant.MerchantLookupService;
import com.ET.exception.BusinessException;
import com.ET.exception.ResourceNotFoundException;
import com.ET.transaction.dto.*;
import com.ET.transaction.entity.Transaction;
import com.ET.transaction.entity.TransactionSource;
import com.ET.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MerchantLookupService merchantLookupService;

    public Page<TransactionResponse> getAll(Long userId, LocalDate startDate, LocalDate endDate,
                                            Long categoryId, Transaction.TransactionType type, TransactionSource source, Long bankAccountId, String search, Pageable pageable) {
        return transactionRepository
                .findWithFilters(userId, startDate, endDate, categoryId, type, source, bankAccountId, search, pageable)
                .map(TransactionResponse::from);
    }

    public TransactionResponse getById(Long id, Long userId) {
        return transactionRepository.findByIdAndUserId(id, userId)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public TransactionResponse create(CreateTransactionRequest request, Long userId) {
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .bankAccountId(request.bankAccountId())
                .transactionDate(request.transactionDate())
                .description(request.description())
                .amount(request.amount())
                .type(request.type())
                .categoryId(request.categoryId())
                .merchantName(request.merchantName())
                .notes(request.notes())
                .isRecurring(false)
                .build();
        transaction = transactionRepository.save(transaction);
        log.info("Transaction created: id={}, userId={}", transaction.getId(), userId);
        return TransactionResponse.from(transaction);
    }

    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public TransactionResponse createCashExpense(CreateCashTransactionRequest request, Long userId) {

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .bankAccountId(null)
                .fileUploadId(null)
                .transactionDate(request.transactionDate())
                .description(request.description())
                .amount(request.amount())
                .type(Transaction.TransactionType.DEBIT)
                .source(TransactionSource.CASH)
                .categoryId(request.categoryId())
                .merchantName(null)
                .merchantId(null)
                .categorySource(CategorizationSource.USER_OVERRIDE)
                .categoryConfidence(100)
                .categoryReason("Manual cash expense")
                .isRecurring(false)
                .notes(request.notes())
                .build();

        transaction = transactionRepository.save(transaction);

        log.info("Cash expense created: id={}, userId={}", transaction.getId(), userId);

        return TransactionResponse.from(transaction);
    }

    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public TransactionResponse update(Long id, UpdateTransactionRequest request, Long userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        if (request.transactionDate() != null) transaction.setTransactionDate(request.transactionDate());
        if (request.description()     != null) transaction.setDescription(request.description());
        if (request.amount()          != null) transaction.setAmount(request.amount());
        if (request.type()            != null) transaction.setType(request.type());
        if (request.merchantName()    != null) transaction.setMerchantName(request.merchantName());
        if (request.notes()           != null) transaction.setNotes(request.notes());

        if (request.categoryId() != null && !request.categoryId().equals(transaction.getCategoryId())) {
            transaction.setCategoryId(request.categoryId());
            transaction.setCategorySource(CategorizationSource.USER_OVERRIDE);
            transaction.setCategoryConfidence(100);
            transaction.setCategoryReason("Manually set by user");

            String merchantKey = transaction.getMerchantName() != null
                    ? transaction.getMerchantName() : transaction.getDescription();
            merchantLookupService.recordUserOverride(merchantKey, request.categoryId());
        }

        return TransactionResponse.from(transactionRepository.save(transaction));
    }

    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public void delete(Long id, Long userId) {
        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
        transactionRepository.delete(transaction);
        log.info("Transaction deleted: id={}, userId={}", id, userId);
    }

    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public int deleteSelected(List<Long> ids, Long userId) {

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("No transactions selected");
        }

        int deleted = transactionRepository.deleteSelected(userId, ids);

        log.info("{} transactions deleted for user {}", deleted, userId);
        return deleted;
    }

    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public int deleteByMonth(int year, int month, Long userId) {

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        int deleted =
                transactionRepository.deleteByDateRange(
                        userId,
                        start,
                        end
                );

        log.info("{} transactions deleted for userId={} in {}-{}", deleted, userId, year, month);
        return deleted;
    }

    @Transactional
    @CacheEvict(value = "analytics", allEntries = true)
    public int deleteByDateRange(
            LocalDate start,
            LocalDate end,
            Long userId
    ) {

        if (start.isAfter(end)) {
            throw new BusinessException("Invalid date range");
        }

        int deleted =
                transactionRepository.deleteByDateRange(
                        userId,
                        start,
                        end
                );

        log.info("{} transactions deleted for userId={} between {} and {}", deleted, userId, start, end);
        return deleted;
    }
}