package com.g42.platform.gms.report.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.report.api.dto.CustomerReportResponse;
import com.g42.platform.gms.report.application.service.CustomerReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Báo cáo khách hàng cho lễ tân / kế toán / quản lý:
 *  - GET /api/reports/customer         → JSON tổng hợp (số khách, tổng thu, phân rã theo khách + phiếu)
 *  - GET /api/reports/customer/export  → file Excel (.xlsx) 3 sheet
 *
 * Mặc định {@code from}/{@code to} là ngày hôm nay.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('RECEPTIONIST', 'ACCOUNTANT', 'MANAGER', 'ADMIN')")
public class CustomerReportController {

    private final CustomerReportService customerReportService;

    @GetMapping("/customer")
    public ResponseEntity<ApiResponse<CustomerReportResponse>> getCustomerReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponses.success(customerReportService.buildReport(from, to)));
    }

    @GetMapping("/customer/export")
    public ResponseEntity<byte[]> exportCustomerReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] content = customerReportService.exportReport(from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "Bao_Cao_Khach_Hang.xlsx");
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
