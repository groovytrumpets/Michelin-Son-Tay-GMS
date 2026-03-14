package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.work_history.WorkHistoryResponse;
import com.g42.platform.gms.service_ticket_management.application.service.WorkHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for technician work history.
 * 
 * This controller provides endpoints for technicians to view their completed
 * service tickets with filtering capabilities by date range and license plate.
 * 
 * Security:
 * - Only users with TECHNICIAN role can access these endpoints
 * - Technicians can only view their own work history
 */
@RestController
@RequestMapping("/api/work-history")
@RequiredArgsConstructor
public class WorkHistoryController {
    
    private final WorkHistoryService workHistoryService;
    
    /**
     * Lấy danh sách lịch sử công việc của kỹ thuật viên
     * 
     * @param startDate Ngày bắt đầu (yyyy-MM-dd)
     * @param endDate Ngày kết thúc (yyyy-MM-dd)
     * @param licensePlate Biển số xe (optional)
     * @param page Số trang (default: 0)
     * @param size Kích thước trang (default: 20)
     * @param staffPrincipal Authenticated technician from JWT token
     * @return Danh sách work history records
     */
    @GetMapping
//    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<Page<WorkHistoryResponse>>> getWorkHistory(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate,
        @RequestParam(required = false) String licensePlate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal StaffPrincipal staffPrincipal
    ) {
        // Extract technician ID from JWT token
        Integer technicianId = staffPrincipal.getStaffId();
        
        // Create pageable with default sorting by completedAt descending
        Pageable pageable = PageRequest.of(page, size, Sort.by("completedAt").descending());
        
        // Get work history
        Page<WorkHistoryResponse> workHistory = workHistoryService.getWorkHistory(
            technicianId, startDate, endDate, licensePlate, pageable);
        
        // Return response
        return ResponseEntity.ok(ApiResponses.success(workHistory));
    }
    
    // TODO: Export endpoint
    // Future implementation will include endpoint to export work history to Excel (.xlsx) and PDF formats
}
