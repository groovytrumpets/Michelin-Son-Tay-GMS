package com.g42.platform.gms.dashboard.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.dashboard.application.service.KpiCalculationService;
import com.g42.platform.gms.dashboard.infrastructure.entity.KpiConfigJpa;
import com.g42.platform.gms.dashboard.infrastructure.repository.KpiConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiCalculationService kpiCalculationService;
    private final KpiConfigRepository kpiConfigRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('" + PermissionCodes.KPI_VIEW + "')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getKpiDashboard(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        
        LocalDate now = LocalDate.now();
        int targetMonth = month != null ? month : now.getMonthValue();
        int targetYear = year != null ? year : now.getYear();
        
        List<Map<String, Object>> dashboard = kpiCalculationService.getKpiDashboardForManager(targetMonth, targetYear);
        return ResponseEntity.ok(ApiResponses.success(dashboard));
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStaffKpiDetails(
            @PathVariable Integer staffId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal StaffPrincipal principal) {
        
        // Xem KPI của người khác thì cần quyền KPI_VIEW; xem của chính mình thì
        // không. Trước đây chỗ này so chuỗi ROLE_MANAGER/ROLE_ADMIN nên nằm ngoài
        // tầm với của màn cấu hình phân quyền — sửa quyền ở /role-permission-config
        // mà endpoint này vẫn chặn theo vai trò cũ.
        boolean canViewOthers = principal.getAuthorities().stream()
                .anyMatch(a -> PermissionCodes.KPI_VIEW.equals(a.getAuthority()));

        if (!canViewOthers && !principal.getStaffId().equals(staffId)) {
            return ResponseEntity.status(403).body(ApiResponses.error("Forbidden", "Bạn không có quyền xem KPI của nhân viên khác."));
        }

        LocalDate now = LocalDate.now();
        int targetMonth = month != null ? month : now.getMonthValue();
        int targetYear = year != null ? year : now.getYear();

        Map<String, Object> details = kpiCalculationService.getKpiDetailsForStaff(staffId, targetMonth, targetYear);
        return ResponseEntity.ok(ApiResponses.success(details));
    }

    @GetMapping("/personal")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPersonalKpi(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal StaffPrincipal principal) {
        
        LocalDate now = LocalDate.now();
        int targetMonth = month != null ? month : now.getMonthValue();
        int targetYear = year != null ? year : now.getYear();

        Map<String, Object> details = kpiCalculationService.getKpiDetailsForStaff(principal.getStaffId(), targetMonth, targetYear);
        return ResponseEntity.ok(ApiResponses.success(details));
    }

    @GetMapping("/configs")
    @PreAuthorize("hasAuthority('" + PermissionCodes.KPI_VIEW + "')")
    public ResponseEntity<ApiResponse<List<KpiConfigJpa>>> getKpiConfigs() {
        List<KpiConfigJpa> configs = kpiConfigRepository.findAll();
        return ResponseEntity.ok(ApiResponses.success(configs));
    }

    @PutMapping("/configs/{configId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.KPI_EDIT + "')")
    public ResponseEntity<ApiResponse<KpiConfigJpa>> updateKpiConfig(
            @PathVariable Integer configId,
            @RequestBody KpiConfigJpa configRequest) {
        
        KpiConfigJpa existing = kpiConfigRepository.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("KPI Config not found with id: " + configId));

        existing.setAttendanceWeight(configRequest.getAttendanceWeight());
        existing.setCompletionWeight(configRequest.getCompletionWeight());
        existing.setSatisfactionWeight(configRequest.getSatisfactionWeight());
        existing.setQualityWeight(configRequest.getQualityWeight());
        
        if (configRequest.getTargetTickets() != null) {
            existing.setTargetTickets(configRequest.getTargetTickets());
        }
        if (configRequest.getTargetHours() != null) {
            existing.setTargetHours(configRequest.getTargetHours());
        }
        if (configRequest.getTargetRating() != null) {
            existing.setTargetRating(configRequest.getTargetRating());
        }

        KpiConfigJpa saved = kpiConfigRepository.save(existing);
        return ResponseEntity.ok(ApiResponses.success(saved));
    }

    @PostMapping("/recalculate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.KPI_EDIT + "')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recalculateKpi(
            @RequestParam Integer staffId,
            @RequestParam int month,
            @RequestParam int year) {
        
        kpiCalculationService.calculateAndSaveKpi(staffId, month, year);
        Map<String, Object> updatedDetails = kpiCalculationService.getKpiDetailsForStaff(staffId, month, year);
        return ResponseEntity.ok(ApiResponses.success(updatedDetails));
    }

    @PostMapping("/recalculate-all")
    @PreAuthorize("hasAuthority('" + PermissionCodes.KPI_EDIT + "')")
    public ResponseEntity<ApiResponse<String>> recalculateAllKpi(
            @RequestParam int month,
            @RequestParam int year) {
        kpiCalculationService.recalculateAllStaff(month, year);
        return ResponseEntity.ok(ApiResponses.success("Đã tính lại KPI cho tất cả nhân viên kỳ " + year + "-" + String.format("%02d", month)));
    }
}
