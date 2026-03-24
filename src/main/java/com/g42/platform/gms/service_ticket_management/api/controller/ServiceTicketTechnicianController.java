package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.technician.TechnicianTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.technician.TechnicianTicketListResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.technician.UpdateTechnicianNotesRequest;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketTechnicianService;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for service ticket management (technician view).
 * Quản lý phiếu dịch vụ cho kỹ thuật viên.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-ticket/technician")
public class ServiceTicketTechnicianController {
    
    private final ServiceTicketTechnicianService technicianService;
    
    /**
     * Get paginated list of service tickets for technician.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param date Filter by received date (optional)
     * @param status Filter by ticket status - case insensitive (optional: DRAFT, CREATED, IN_PROGRESS, COMPLETED, CANCELLED)
     * @param search Search by ticket code, customer name, phone, or license plate (optional)
     * @return Page of TechnicianTicketListResponse
     */
    @GetMapping("/tickets")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<Page<TechnicianTicketListResponse>>> getTechnicianTicketList(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        
        Integer staffId = principal.getStaffId();

        // Parse status string to enum (case-insensitive)
        TicketStatus ticketStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                ticketStatus = TicketStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                ticketStatus = null;
            }
        }
        
        Page<TechnicianTicketListResponse> tickets = technicianService.getTechnicianTicketList(
            staffId, page, size, date, ticketStatus, search);
        
        return ResponseEntity.ok(ApiResponses.success(tickets));
    }
    
    /**
     * Get service ticket detail for technician.
     * 
     * @param ticketCode Ticket code (ST_XXXXXX or MST_XXXXXX)
     * @return TechnicianTicketDetailResponse with full information
     */
    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<ApiResponse<TechnicianTicketDetailResponse>> getTechnicianTicketDetail(
            @PathVariable String ticketCode) {
        
        TechnicianTicketDetailResponse detail = technicianService.getTechnicianTicketDetail(ticketCode);
        
        return ResponseEntity.ok(ApiResponses.success(detail));
    }
    
    /**
     * Update technician notes.
     */
    @PutMapping("/tickets/{ticketCode}/notes")
    public ResponseEntity<ApiResponse<TechnicianTicketDetailResponse>> updateTechnicianNotes(
            @PathVariable String ticketCode,
            @RequestBody @jakarta.validation.Valid UpdateTechnicianNotesRequest request) {

        TechnicianTicketDetailResponse updated = technicianService.updateTechnicianNotes(ticketCode, request);
        return ResponseEntity.ok(ApiResponses.success(updated));
    }

    /**
     * Technician bắt đầu sửa xe — PENDING → IN_PROGRESS.
     */
    @PostMapping("/tickets/{ticketCode}/start")
    public ResponseEntity<ApiResponse<TechnicianTicketDetailResponse>> startWork(
            @PathVariable String ticketCode) {

        TechnicianTicketDetailResponse result = technicianService.startWork(ticketCode);
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    /**
     * Technician báo thiếu phụ tùng — IN_PROGRESS → PENDING.
     */
    @PostMapping("/tickets/{ticketCode}/wait-parts")
    public ResponseEntity<ApiResponse<TechnicianTicketDetailResponse>> waitForParts(
            @PathVariable String ticketCode,
            @RequestBody(required = false) com.g42.platform.gms.service_ticket_management.api.dto.technician.WaitPartsRequest request) {

        TechnicianTicketDetailResponse result = technicianService.waitForParts(ticketCode, request);
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    /**
     * Technician báo xong sửa xe — IN_PROGRESS → COMPLETED.
     */
    @PostMapping("/tickets/{ticketCode}/finish")
    public ResponseEntity<ApiResponse<TechnicianTicketDetailResponse>> finishWork(
            @PathVariable String ticketCode) {

        TechnicianTicketDetailResponse result = technicianService.finishWork(ticketCode);
        return ResponseEntity.ok(ApiResponses.success(result));
    }
}
