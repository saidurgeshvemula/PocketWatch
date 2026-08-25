package com.ET.ai.insights.controller;

import com.ET.ai.insights.service.AIInsightsService;
import com.ET.common.Response.ApiResponse;
import com.ET.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/ai/insights")
@RequiredArgsConstructor
public class AIInsightsController {

    private final AIInsightsService aiInsightsService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<ApiResponse<String>> getInsights(
            @RequestParam String month,
            @RequestParam(required = false) Long bankAccountId
    ) {

        Long userId = securityUtils.getCurrentUserId();

        YearMonth selectedMonth = YearMonth.parse(month);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Insights generated",
                        aiInsightsService.generateInsights(
                                userId,
                                selectedMonth,
                                bankAccountId
                        )
                )
        );
    }
}