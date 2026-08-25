package com.ET.ai.chat.controller;

import com.ET.ai.chat.dto.ChatDTOs;
import com.ET.ai.chat.service.AIChatService;
import com.ET.common.Response.ApiResponse;
import com.ET.common.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatDTOs.ChatResponse>> chat(
            @RequestBody @Valid ChatDTOs.ChatRequest request
    ) {

        Long userId =
                securityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Reply generated",
                        aiChatService.chat(
                                userId,
                                request.message(),
                                request.bankAccountId()
                        )
                )
        );
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatDTOs.ChatHistoryItem>>> getHistory() {

        Long userId =
                securityUtils.getCurrentUserId();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "History fetched",
                        aiChatService.getHistory(userId)
                )
        );
    }

    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearHistory() {

        Long userId =
                securityUtils.getCurrentUserId();

        aiChatService.clearHistory(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "History cleared"
                )
        );
    }
}