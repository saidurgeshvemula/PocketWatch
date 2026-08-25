package com.ET.transaction.controller;

import com.ET.common.Response.ApiResponse;
import com.ET.common.Response.PagedResponse;
import com.ET.common.util.SecurityUtils;
import com.ET.transaction.dto.*;
import com.ET.transaction.entity.Transaction;
import com.ET.transaction.entity.TransactionSource;
import com.ET.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<TransactionResponse>>> getAll(

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd/MM/yyyy")
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "dd/MM/yyyy")
            LocalDate endDate,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            Transaction.TransactionType type,

            @RequestParam(required = false)
            TransactionSource source,

            @RequestParam(required = false)
            Long bankAccountId,

            @RequestParam(required = false)
            String search,

            @PageableDefault(
                    size = 20,
                    sort = "transactionDate",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {

        Long userId = securityUtils.getCurrentUserId();

        Page<TransactionResponse> page = transactionService.getAll(
                userId,
                startDate,
                endDate,
                categoryId,
                type,
                source,
                bankAccountId,
                search,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Transactions fetched",
                        PagedResponse.from(page)
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("Transaction fetched", transactionService.getById(id, userId))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @RequestBody @Valid CreateTransactionRequest request
    ) {
        Long userId = securityUtils.getCurrentUserId();
        TransactionResponse response = transactionService.create(request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created", response));
    }

    @PostMapping("/cash")
    public ResponseEntity<ApiResponse<TransactionResponse>> createCashExpense(
            @RequestBody @Valid CreateCashTransactionRequest request
    ) {

        Long userId = securityUtils.getCurrentUserId();

        TransactionResponse response =
                transactionService.createCashExpense(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cash expense added", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateTransactionRequest request
    ) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("Transaction updated", transactionService.update(id, request, userId))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        transactionService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted"));
    }

    @DeleteMapping("/month")
    public ResponseEntity<ApiResponse<Integer>> deleteByMonth(
            @RequestBody @Valid DeleteMonthRequest request
    ) {
        Long userId = securityUtils.getCurrentUserId();
        int deleted = transactionService.deleteByMonth(request.year(), request.month(), userId);

        String message = deleted == 0
                ? "No transactions found for the selected month"
                : deleted + " transaction(s) deleted";

        return ResponseEntity.ok(ApiResponse.success(message, deleted));
    }

    @DeleteMapping("/date-range")
    public ResponseEntity<ApiResponse<Integer>> deleteByDateRange(
            @RequestBody @Valid DeleteDateRangeRequest request
    ) {
        Long userId = securityUtils.getCurrentUserId();
        int deleted = transactionService.deleteByDateRange(request.startDate(), request.endDate(), userId);

        String message = deleted == 0
                ? "No transactions found in the selected date range"
                : deleted + " transaction(s) deleted";

        return ResponseEntity.ok(ApiResponse.success(message, deleted));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Integer>> deleteSelected(
            @RequestBody @Valid BulkDeleteRequest request
    ) {
        Long userId = securityUtils.getCurrentUserId();
        int deleted = transactionService.deleteSelected(request.transactionIds(), userId);

        String message = deleted == 0
                ? "No matching transactions found"
                : deleted + " transaction(s) deleted";

        return ResponseEntity.ok(ApiResponse.success(message, deleted));
    }
}