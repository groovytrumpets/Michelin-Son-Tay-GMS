package com.g42.platform.gms.report.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.report.api.dto.RevenueReportResponse;
import com.g42.platform.gms.report.application.service.RevenueReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Dữ liệu trang Quản lý doanh thu:
 *  - GET /api/reports/revenue?from=&to=&includeLegacy= → tổng hợp + danh sách giao dịch theo hoá đơn.
 *
 * Mặc định {@code from} là ngày đầu tháng, {@code to} là hôm nay; {@code includeLegacy} mặc định
 * false vì tiền sổ cũ là số chép tay, không phải hoá đơn.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCodes.REVENUE_VIEW + "')")
public class RevenueReportController {

    private final RevenueReportService revenueReportService;

    @GetMapping("/revenue")
    public ResponseEntity<ApiResponse<RevenueReportResponse>> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "false") boolean includeLegacy,
            // null = mọi xưởng
            @RequestParam(required = false) Integer branchId) {
        return ResponseEntity.ok(ApiResponses.success(
                revenueReportService.buildReport(from, to, includeLegacy, branchId)));
    }
}
