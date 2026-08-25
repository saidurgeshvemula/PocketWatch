package com.ET.transaction.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCashTransactionRequest(

        @NotNull
        LocalDate transactionDate,

        @NotBlank
        String description,

        @NotNull
        @Positive
        BigDecimal amount,

        @NotNull
        Long categoryId,

        String notes
) {}
