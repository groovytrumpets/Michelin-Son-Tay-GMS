package com.g42.platform.gms.systemlog.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.systemlog.dto.SystemLogDto;
import com.g42.platform.gms.systemlog.dto.SystemLogFilter;
import com.g42.platform.gms.systemlog.dto.SystemLogFilterOptionsDto;
import com.g42.platform.gms.systemlog.dto.SystemLogStatsDto;
import com.g42.platform.gms.systemlog.service.SystemLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bộ lọc (startDate, endDate, role, action, severity, module, search, actorStaffId,
 * operation, hideNoise, targetKind, targetId) bind vào SystemLogFilter từ query string.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/system-logs")
@PreAuthorize("hasAuthority('" + PermissionCodes.SYSTEM_LOG_VIEW + "')")
public class SystemLogController {

    private final SystemLogQueryService systemLogQueryService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SystemLogDto>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @ModelAttribute SystemLogFilter filter) {
        return ResponseEntity.ok(ApiResponses.success(systemLogQueryService.search(page, size, filter)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SystemLogStatsDto>> getStats(@ModelAttribute SystemLogFilter filter) {
        return ResponseEntity.ok(ApiResponses.success(systemLogQueryService.stats(filter)));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<SystemLogFilterOptionsDto>> getFilterOptions() {
        return ResponseEntity.ok(ApiResponses.success(systemLogQueryService.filterOptions()));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(@ModelAttribute SystemLogFilter filter) {
        byte[] csv = systemLogQueryService.exportCsv(filter);
        String filename = "nhat-ky-he-thong_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(csv);
    }
}
