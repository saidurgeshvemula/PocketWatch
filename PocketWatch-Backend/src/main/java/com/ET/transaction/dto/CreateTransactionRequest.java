package com.ET.transaction.dto;

import com.ET.transaction.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(

        @NotNull(message = "Transaction date is required")
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate transactionDate,

        @NotBlank(message = "Description is required")
        @Size(max = 512, message = "Description must not exceed 512 characters")
        String description,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 13, fraction = 2, message = "Amount format is invalid")
        BigDecimal amount,

        @NotNull(message = "Transaction type is required")
        Transaction.TransactionType type,

        Long categoryId,

        Long bankAccountId,

        String merchantName,

        String notes
) {}
