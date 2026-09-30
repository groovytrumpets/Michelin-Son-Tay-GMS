package com.g42.platform.gms.dashboard.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.dashboard.api.dto.DashboardSummaryResponse;
import com.g42.platform.gms.dashboard.application.service.DashboardSummaryService;
import com.g42.platform.gms.dashboard.application.service.DashboardSummaryService.Access;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Số liệu tổng hợp cho trang /dashboard.
 *  - GET  /api/dashboard/summary?from=&to=         → số liệu kỳ (bỏ from = kỳ "Tất cả").
 *  - POST /api/dashboard/summary/refresh?from=&to= → tính lại cache của kỳ (REVENUE_VIEW).
 *
 * Mọi nhân viên đều gọi được GET; nhóm doanh thu / lịch hẹn / khách hàng chỉ có trong phản hồi
 * khi người xem có quyền xem tương ứng, nên dashboard của KTV không lộ doanh thu.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardSummaryService dashboardSummaryService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Set<String> authorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        Access access = new Access(
                authorities.contains(PermissionCodes.REVENUE_VIEW),
                authorities.contains(PermissionCodes.BOOKING_VIEW),
                authorities.contains(PermissionCodes.CUSTOMER_VIEW));
        return ResponseEntity.ok(ApiResponses.success(
                dashboardSummaryService.getSummary(from, to, principal.getStaffId(), access)));
    }

    @PostMapping("/summary/refresh")
    @PreAuthorize("hasAuthority('" + PermissionCodes.REVENUE_VIEW + "')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> refresh(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        int days = dashboardSummaryService.refresh(from, to);
        return ResponseEntity.ok(ApiResponses.success(Map.of("days", days)));
    }
}
