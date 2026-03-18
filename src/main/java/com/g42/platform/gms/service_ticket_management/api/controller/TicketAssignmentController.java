package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.RoleDto;
import com.g42.platform.gms.service_ticket_management.application.service.TicketAssignmentService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/service-ticket/assignment/")
public class TicketAssignmentController {
    private final TicketAssignmentService ticketAssignmentService;

    @GetMapping("{ticketId}/available-staff")
    public ResponseEntity<ApiResponse<List<AvailableStaffDto>>> getAvailableStaff(@PathVariable Integer ticketId,
                                                                                  @RequestParam String role) {
        return ResponseEntity.ok(ApiResponses.success(ticketAssignmentService.getAvailableStaff(ticketId, role)));
    }
    @PostMapping("{ticketId}/assign")
    public ResponseEntity<ApiResponse<AssignStaffDto>> assignStaff(
            @PathVariable Integer ticketId,
            @RequestBody AssignStaffDto dto) {
        return ResponseEntity.ok(ApiResponses.success(ticketAssignmentService.assignStaff(ticketId, dto)));
    }
}
