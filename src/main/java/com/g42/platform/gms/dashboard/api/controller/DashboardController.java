package com.g42.platform.gms.dashboard.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.dashboard.api.dto.DashboardRevenueResponseDto;
import com.g42.platform.gms.dashboard.application.service.DashboardRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardRevenueService dashboardRevenueService;

    /**
     * GET /api/dashboard/revenue?from=2026-01-01&to=2026-12-31
     * Lay bao cao tong hop doanh thu tu bang summary table (materialized view pattern).
     */
    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<DashboardRevenueResponseDto>> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        DashboardRevenueResponseDto report = dashboardRevenueService.getRevenueReport(from, to);
        return ResponseEntity.ok(ApiResponses.success(report));
    }
}
