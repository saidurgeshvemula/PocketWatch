package com.ET.analytics.controller;

import com.ET.analytics.dto.CategoryBreakdown;
import com.ET.analytics.dto.MerchantSummary;
import com.ET.analytics.dto.MonthlySummary;
import com.ET.analytics.service.AnalyticsService;
import com.ET.common.Response.ApiResponse;
import com.ET.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final SecurityUtils securityUtils;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<MonthlySummary>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate endDate,
            @RequestParam(required = false) Long bankAccountId
    ) {
        Long userId = securityUtils.getCurrentUserId();
        LocalDate start = startDate != null ? startDate : YearMonth.now().atDay(1);
        LocalDate end   = endDate   != null ? endDate   : YearMonth.now().atEndOfMonth();
        return ResponseEntity.ok(ApiResponse.success("Summary fetched", analyticsService.getSummary(userId, start, end, bankAccountId)));
    }

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<List<MonthlySummary>>> getMonthly(
            @RequestParam(required = false) Long bankAccountId
    ) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Monthly data fetched", analyticsService.getMonthly(userId, bankAccountId)));
    }

    @GetMapping("/by-category")
    public ResponseEntity<ApiResponse<List<CategoryBreakdown>>> getByCategory(
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate endDate,
            @RequestParam(required = false) Long bankAccountId
    ) {
        Long userId = securityUtils.getCurrentUserId();
        LocalDate start = startDate != null ? startDate : YearMonth.now().atDay(1);
        LocalDate end   = endDate   != null ? endDate   : YearMonth.now().atEndOfMonth();
        return ResponseEntity.ok(ApiResponse.success("Category breakdown fetched", analyticsService.getByCategory(userId, start, end, bankAccountId)));
    }

    @GetMapping("/top-merchants")
    public ResponseEntity<ApiResponse<List<MerchantSummary>>> getTopMerchants(
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate endDate,
            @RequestParam(required = false) Long bankAccountId
    ) {
        Long userId = securityUtils.getCurrentUserId();
        LocalDate start = startDate != null ? startDate : YearMonth.now().atDay(1);
        LocalDate end   = endDate   != null ? endDate   : YearMonth.now().atEndOfMonth();
        return ResponseEntity.ok(ApiResponse.success("Top merchants fetched", analyticsService.getTopMerchants(userId, start, end, bankAccountId)));
    }
}