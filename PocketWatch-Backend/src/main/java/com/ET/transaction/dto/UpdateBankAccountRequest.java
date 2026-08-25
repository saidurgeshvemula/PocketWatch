package com.ET.transaction.dto;

import com.ET.transaction.entity.BankAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateBankAccountRequest(

        @NotBlank(message = "Nickname is required")
        @Size(max = 255, message = "Nickname must not exceed 255 characters")
        String nickname,

        @NotBlank(message = "Bank name is required")
        @Size(max = 255, message = "Bank name must not exceed 255 characters")
        String bankName,

        @Size(max = 20, message = "Account number must not exceed 20 characters")
        String accountNumber,

        @NotNull(message = "Account type is required")
        BankAccount.AccountType accountType
) {}
