package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.StaffWorkloadDto;
import com.g42.platform.gms.service_ticket_management.application.service.TicketAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller để xem workload của staff - giúp quản lý phân công công việc
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff-workload")
public class StaffWorkloadController {

    private final TicketAssignmentService ticketAssignmentService;

    /**
     * Lấy workload của tất cả staff theo role
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffWorkloadDto>>> getStaffWorkload(
            @RequestParam(required = false) String role) {
        List<StaffWorkloadDto> workload = ticketAssignmentService.getStaffWorkload(role);
        return ResponseEntity.ok(ApiResponses.success(workload));
    }

    /**
     * Lấy workload của một staff cụ thể
     */
    @GetMapping("/{staffId}")
    public ResponseEntity<ApiResponse<StaffWorkloadDto>> getStaffWorkload(@PathVariable Integer staffId) {
        StaffWorkloadDto workload = ticketAssignmentService.getStaffWorkload(staffId);
        return ResponseEntity.ok(ApiResponses.success(workload));
    }

    /**
     * Kiểm tra staff có thể assign thêm không
     */
    @GetMapping("/{staffId}/can-assign")
    public ResponseEntity<ApiResponse<Boolean>> canAssignStaff(
            @PathVariable Integer staffId,
            @RequestParam(defaultValue = "0") Integer ticketId) {
        boolean canAssign = ticketAssignmentService.canAssignStaff(staffId, ticketId);
        return ResponseEntity.ok(ApiResponses.success(canAssign));
    }
}