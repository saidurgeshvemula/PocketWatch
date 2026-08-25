package com.ET.transaction.service;

import com.ET.exception.ResourceNotFoundException;
import com.ET.transaction.dto.BankAccountResponse;
import com.ET.transaction.dto.CreateBankAccountRequest;
import com.ET.transaction.dto.UpdateBankAccountRequest;
import com.ET.transaction.entity.BankAccount;
import com.ET.transaction.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public List<BankAccountResponse> getAllByUser(Long userId) {
        return bankAccountRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(BankAccountResponse::from)
                .toList();
    }

    public BankAccountResponse getById(Long id, Long userId) {
        return bankAccountRepository.findByIdAndUserId(id, userId)
                .map(BankAccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account", id));
    }

    @Transactional
    public BankAccountResponse create(CreateBankAccountRequest request, Long userId) {

        BankAccount account = BankAccount.builder()
                .userId(userId)
                .nickname(request.nickname())
                .bankName(request.bankName())
                .accountType(request.accountType())
                .currency("INR")
                .build();

        if (request.accountNumber() != null && !request.accountNumber().isBlank()) {
            String number = request.accountNumber();

            account.setAccountNumberMasked(
                    number.length() > 4
                            ? "********" + number.substring(number.length() - 4)
                            : number
            );
        }

        account = bankAccountRepository.save(account);

        log.info("Bank account created: id={}, userId={}", account.getId(), userId);

        return BankAccountResponse.from(account);
    }

    @Transactional
    public BankAccountResponse update(Long id, UpdateBankAccountRequest request, Long userId) {

        BankAccount account = bankAccountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account", id));

        if (request.nickname() != null) {
            account.setNickname(request.nickname());
        }

        if (request.bankName() != null) {
            account.setBankName(request.bankName());
        }

        if (request.accountType() != null) {
            account.setAccountType(request.accountType());
        }

        if (request.accountNumber() != null && !request.accountNumber().isBlank()) {

            String number = request.accountNumber();

            account.setAccountNumberMasked(
                    number.length() > 4
                            ? "********" + number.substring(number.length() - 4)
                            : number
            );
        }

        account = bankAccountRepository.save(account);

        log.info("Bank account updated: id={}, userId={}", account.getId(), userId);

        return BankAccountResponse.from(account);
    }

    @Transactional
    public void delete(Long id, Long userId) {

        BankAccount account = bankAccountRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account", id));

        bankAccountRepository.delete(account);

        log.info("Bank account deleted: id={}, userId={}", id, userId);
    }
}