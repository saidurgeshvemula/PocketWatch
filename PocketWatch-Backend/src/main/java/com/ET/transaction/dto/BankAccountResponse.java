package com.ET.transaction.dto;

import com.ET.transaction.entity.BankAccount;

import java.time.LocalDateTime;

public record BankAccountResponse(
        Long id,
        String nickname,
        String bankName,
        String accountNumberMasked,
        BankAccount.AccountType accountType,
        LocalDateTime createdAt
) {
    public static BankAccountResponse from(BankAccount account) {
        return new BankAccountResponse(
                account.getId(),
                account.getNickname(),
                account.getBankName(),
                account.getAccountNumberMasked(),
                account.getAccountType(),
                account.getCreatedAt()
        );
    }
}