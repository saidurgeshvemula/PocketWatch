package com.ET.ai.controller;

import com.ET.categorization.engine.CategorizationEngine;
import com.ET.common.Response.ApiResponse;
import com.ET.common.util.SecurityUtils;
import com.ET.exception.ResourceNotFoundException;
import com.ET.transaction.entity.Transaction;
import com.ET.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final CategorizationEngine categorizationEngine;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;

    @PostMapping("/transactions/{id}/recategorize")
    public ResponseEntity<ApiResponse<String>> recategorize(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();

        Transaction transaction = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));

        String category = categorizationEngine.recategorizeAndSave(transaction);

        return ResponseEntity.ok(
                ApiResponse.success("Transaction recategorized as: " + category)
        );
    }
}
