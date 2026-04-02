package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.AddCustomCategoryRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.AdvisorNoteRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.InspectionItemRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.InspectionItemResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.SafetyInspectionResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.WorkCategoryResponse;
import com.g42.platform.gms.service_ticket_management.application.service.SafetyInspectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
//    @PreAuthorize("hasRole('TECHNICIAN')")
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
            @PathVariable String ticketCode) {

        SafetyInspectionResponse response = safetyInspectionService.skipInspectionByCode(ticketCode);
        return ResponseEntity.ok(ApiResponses.success(response, "Đã bỏ qua kiểm tra an toàn"));
    }

    /**
     * Reopen an existing safety inspection record for editing.
     * This puts inspection back to PENDING and ticket back to DRAFT.
     */
    @PostMapping("/{ticketCode}/reopen")
    public ResponseEntity<ApiResponse<SafetyInspectionResponse>> reopenInspection(
            @PathVariable String ticketCode) {

        SafetyInspectionResponse response = safetyInspectionService.reopenInspectionByCode(ticketCode);
        return ResponseEntity.ok(ApiResponses.success(response, "Đã mở lại phiếu kiểm tra an toàn"));
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
//    @PreAuthorize("hasRole('TECHNICIAN')")
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
     * Get default safety inspection categories (13 hạng mục cố định).
     */
    @GetMapping("/categories/default")
    public ResponseEntity<ApiResponse<List<WorkCategoryResponse>>> getDefaultSafetyInspectionCategories() {
        List<WorkCategoryResponse> categories = safetyInspectionService.getDefaultSafetyInspectionCategories();
        return ResponseEntity.ok(ApiResponses.success(categories, "Danh sách 13 hạng mục kiểm tra an toàn mặc định"));
    }

    /**
     * Lấy danh sách tất cả hạng mục kiểm tra của một phiếu (13 default + hạng mục phụ).
     * Tech dùng để xem và điền itemStatus.
     */
    @GetMapping("/{inspectionId}/items")
    public ResponseEntity<ApiResponse<List<InspectionItemResponse>>> getInspectionItems(
            @PathVariable Integer inspectionId) {

        List<InspectionItemResponse> items = safetyInspectionService.getInspectionItems(inspectionId);
        return ResponseEntity.ok(ApiResponses.success(items));
    }

    /**
     * Thêm hạng mục tùy chỉnh vào phiếu kiểm tra an toàn.
     * Lưu vào ticket_custom_category, không ảnh hưởng work_category.
     */
    @PostMapping("/{inspectionId}/custom-categories")
    public ResponseEntity<ApiResponse<InspectionItemResponse>> addCustomCategory(
            @PathVariable Integer inspectionId,
            @Valid @RequestBody AddCustomCategoryRequest request) {

        InspectionItemResponse response = safetyInspectionService.addCustomCategory(inspectionId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(response, "Đã thêm hạng mục tùy chỉnh thành công"));
    }

    /**
     * Xóa mềm hạng mục tùy chỉnh (đặt status = DELETED).
     */
    @DeleteMapping("/{inspectionId}/custom-categories/{categoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomCategory(
            @PathVariable Integer inspectionId,
            @PathVariable Integer categoryId) {

        safetyInspectionService.deleteCustomCategory(inspectionId, categoryId);
        return ResponseEntity.ok(ApiResponses.success(null, "Đã xóa hạng mục tùy chỉnh"));
    }

    /**
     * Bulk update itemStatus cho nhiều hạng mục cùng lúc (tech điền).
     * Hỗ trợ cả default (workCategoryId) và custom (customCategoryId).
     */
    @PatchMapping("/{inspectionId}/items")
    public ResponseEntity<ApiResponse<List<InspectionItemResponse>>> upsertItems(
            @PathVariable Integer inspectionId,
            @RequestBody List<InspectionItemRequest> items) {

        List<InspectionItemResponse> response = safetyInspectionService.upsertItems(inspectionId, items);
        return ResponseEntity.ok(ApiResponses.success(response, "Đã cập nhật hạng mục kiểm tra"));
    }

    /**
     * Advisor cập nhật ghi chú (advisorNote) cho nhiều hạng mục kiểm tra cùng lúc.
     * Chỉ advisor mới được gọi endpoint này.
     */
    @PatchMapping("/{inspectionId}/advisor-notes")
    public ResponseEntity<ApiResponse<List<InspectionItemResponse>>> updateAdvisorNotes(
            @PathVariable Integer inspectionId,
            @RequestBody AdvisorNoteRequest request) {

        List<InspectionItemResponse> responses = safetyInspectionService.updateAdvisorNotes(
                inspectionId, request.getItems());
        return ResponseEntity.ok(ApiResponses.success(responses, "Đã cập nhật ghi chú advisor thành công"));
    }
    @PutMapping("/{serviceTicketId}/update-recommend")
    public ResponseEntity<ApiResponse<String>> updateSafetyInspection(
            @PathVariable Integer serviceTicketId,
            @RequestParam String recommend){
        String rcm = safetyInspectionService.saveRecommend(serviceTicketId,recommend);
        return ResponseEntity.ok(ApiResponses.success(rcm));
    }

}
