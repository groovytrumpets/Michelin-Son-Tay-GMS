package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.assignment.TicketAssignmentResponse;
import com.g42.platform.gms.service_ticket_management.application.service.TicketAssignmentService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/service-ticket/assignment/")
public class TicketAssignmentController {
    TicketAssignmentService ticketAssignmentService;
    @GetMapping("/{serviceTicketId}/assignments")
    public ResponseEntity<ApiResponse<TicketAssignmentResponse>> getAssignments(
            @PathVariable Integer serviceTicketId) {

        TicketAssignmentResponse response = ticketAssignmentService.getAssignments(serviceTicketId);
        return ResponseEntity.ok(ApiResponses.success(response));
    }
}
