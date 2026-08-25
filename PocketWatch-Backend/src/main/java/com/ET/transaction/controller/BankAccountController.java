package com.ET.transaction.controller;

import com.ET.common.Response.ApiResponse;
import com.ET.common.util.SecurityUtils;
import com.ET.transaction.dto.*;
import com.ET.transaction.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BankAccountResponse>>> getAll() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("Accounts fetched", bankAccountService.getAllByUser(userId))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountResponse>> getById(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("Account fetched", bankAccountService.getById(id, userId))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BankAccountResponse>> create(
            @RequestBody @Valid CreateBankAccountRequest request
    ) {
        Long userId = securityUtils.getCurrentUserId();
        BankAccountResponse response = bankAccountService.create(request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BankAccountResponse>> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateBankAccountRequest request
    ) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("Account updated", bankAccountService.update(id, request, userId))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        bankAccountService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Account deleted"));
    }
}
