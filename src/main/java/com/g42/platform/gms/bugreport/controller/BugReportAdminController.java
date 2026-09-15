package com.g42.platform.gms.bugreport.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.bugreport.dto.BugReportDto;
import com.g42.platform.gms.bugreport.dto.BugReportStatsDto;
import com.g42.platform.gms.bugreport.dto.BugReportUpdateRequest;
import com.g42.platform.gms.bugreport.service.BugReportQueryService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Màn hình quản lý báo lỗi phần mềm — chỉ ADMIN. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/bug-reports")
@PreAuthorize("hasAuthority('" + PermissionCodes.BUG_REPORT_VIEW + "')")
public class BugReportAdminController {

    private final BugReportQueryService bugReportQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BugReportDto>>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String reporterType,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponses.success(bugReportQueryService.search(
                page, size, startDate, endDate, status, severity, category, module, reporterType, search)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<BugReportStatsDto>> getStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String reporterType,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponses.success(bugReportQueryService.stats(
                startDate, endDate, status, severity, category, module, reporterType, search)));
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<BugReportDto>> getReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(ApiResponses.success(bugReportQueryService.getById(reportId)));
    }

    @PutMapping("/{reportId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.BUG_REPORT_EDIT + "')")
    public ResponseEntity<ApiResponse<BugReportDto>> updateReport(@PathVariable Long reportId,
                                                                  @Valid @RequestBody BugReportUpdateRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                bugReportQueryService.update(reportId, request), "Đã cập nhật phiếu báo lỗi."));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String reporterType,
            @RequestParam(required = false) String search) {
        byte[] csv = bugReportQueryService.exportCsv(
                startDate, endDate, status, severity, category, module, reporterType, search);
        String filename = "bao-loi-phan-mem_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
