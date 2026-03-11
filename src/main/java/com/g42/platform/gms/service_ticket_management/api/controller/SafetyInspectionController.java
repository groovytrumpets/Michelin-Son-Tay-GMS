package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.WorkCategoryResponse;
import com.g42.platform.gms.service_ticket_management.application.service.SafetyInspectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety-inspections")
@RequiredArgsConstructor
public class SafetyInspectionController {

    private final SafetyInspectionService safetyInspectionService;

    /**
     * Enable safety inspection for a service ticket
     * Technician ID is extracted from JWT token
     * Only TECHNICIAN role can access this endpoint
     */
    @PreAuthorize("hasRole('TECHNICIAN')")
    @PostMapping("/{ticketCode}/enable")
    public ResponseEntity<ApiResponse<SafetyInspectionResponse>> enableInspection(
            @PathVariable String ticketCode,
            @AuthenticationPrincipal StaffPrincipal principal) {
        
        Integer technicianId = principal.getStaffId();
        if (technicianId == null) {
            throw new IllegalStateException("Staff profile not found in JWT token. Please login again.");
        }
        
        SafetyInspectionResponse response = safetyInspectionService.enableInspectionByCode(ticketCode, technicianId);
        return ResponseEntity.ok(ApiResponses.success(response, "Đã kích hoạt kiểm tra an toàn thành công"));
    }

    /**
     * Skip safety inspection for a service ticket
     */
    @PostMapping("/{ticketCode}/skip")
    public ResponseEntity<ApiResponse<SafetyInspectionResponse>> skipInspection(
            @PathVariable String ticketCode,
            @RequestParam(required = false) String reason) {
        
        SafetyInspectionResponse response = safetyInspectionService.skipInspectionByCode(ticketCode, reason);
        return ResponseEntity.ok(ApiResponses.success(response, "Safety inspection skipped successfully"));
    }

    /**
     * Get safety inspection details by inspection ID
     */
    @GetMapping("/{inspectionId}")
    public ResponseEntity<ApiResponse<SafetyInspectionResponse>> getInspectionDetails(
            @PathVariable Integer inspectionId) {
        
        SafetyInspectionResponse response = safetyInspectionService.getInspectionById(inspectionId);
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    /**
     * Save safety inspection data (create or update COMPLETED record)
     * Technician ID is extracted from JWT token
     * Only TECHNICIAN role can access this endpoint
     */
    @PreAuthorize("hasRole('TECHNICIAN')")
    @PostMapping
    public ResponseEntity<ApiResponse<SafetyInspectionResponse>> saveInspectionData(
            @RequestBody SafetyInspectionRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        
        Integer technicianId = principal.getStaffId();
        if (technicianId == null) {
            throw new IllegalStateException("Staff profile not found in JWT token. Please login again.");
        }
        
        SafetyInspectionResponse response = safetyInspectionService.saveInspectionData(request, technicianId);
        return ResponseEntity.ok(ApiResponses.success(response, "Đã lưu dữ liệu kiểm tra an toàn thành công"));
    }

    /**
     * Get safety inspection by service ticket code
     */
    @GetMapping("/service-ticket/{ticketCode}")
    public ResponseEntity<ApiResponse<SafetyInspectionResponse>> getInspectionByServiceTicket(
            @PathVariable String ticketCode) {
        
        SafetyInspectionResponse response = safetyInspectionService.getInspectionByTicketCode(ticketCode);
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    /**
     * Update safety inspection data
     */
    @PutMapping("/{inspectionId}")
    public ResponseEntity<ApiResponse<SafetyInspectionResponse>> updateInspectionData(
            @PathVariable Integer inspectionId,
            @RequestBody SafetyInspectionRequest request) {
        
        SafetyInspectionResponse response = safetyInspectionService.updateInspectionData(inspectionId, request);
        return ResponseEntity.ok(ApiResponses.success(response, "Safety inspection updated successfully"));
    }

    /**
     * Get available safety inspection categories from work_category table
     */
    @PreAuthorize("hasRole('TECHNICIAN')")

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<WorkCategoryResponse>>> getSafetyInspectionCategories() {
        
        List<WorkCategoryResponse> categories = safetyInspectionService.getSafetyInspectionCategories();
        return ResponseEntity.ok(ApiResponses.success(categories, "Danh sách hạng mục kiểm tra an toàn"));
    }

}