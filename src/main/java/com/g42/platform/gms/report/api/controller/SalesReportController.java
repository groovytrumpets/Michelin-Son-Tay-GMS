package com.g42.platform.gms.report.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.report.api.dto.SalesReportResponse;
import com.g42.platform.gms.report.application.service.SalesReportService;
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
 * Báo cáo doanh thu theo khách hàng & theo sản phẩm (trang /sales-report):
 *  - GET /api/reports/sales?from=&to=&status=&customerKey=&productKey=&includeLegacy=&branchId=
 *
 * Cùng quyền với trang Quản lý doanh thu vì cùng là số tiền thu. Mặc định {@code from} là ngày
 * đầu tháng, {@code to} là hôm nay, {@code status=ALL}, sổ cũ không gộp.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCodes.REVENUE_VIEW + "')")
public class SalesReportController {

    private final SalesReportService salesReportService;

    @GetMapping("/sales")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            // ALL | PAID | UNPAID
            @RequestParam(required = false) String status,
            // khoá lấy từ customers[].customerKey / products[].productKey của chính báo cáo này
            @RequestParam(required = false) String customerKey,
            @RequestParam(required = false) String productKey,
            @RequestParam(defaultValue = "false") boolean includeLegacy,
            // null = mọi xưởng
            @RequestParam(required = false) Integer branchId) {
        return ResponseEntity.ok(ApiResponses.success(salesReportService.buildReport(
                from, to, status, customerKey, productKey, includeLegacy, branchId)));
    }
}
