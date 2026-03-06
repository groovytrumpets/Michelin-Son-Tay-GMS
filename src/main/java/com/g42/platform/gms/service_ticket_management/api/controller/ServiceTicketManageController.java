package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketListResponse;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketManageService;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for service ticket management (receptionist view).
 * Tương tự BookingManageController trong booking_management package.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-ticket/manage")
public class ServiceTicketManageController {
    
    private final ServiceTicketManageService serviceTicketManageService;
    
    /**
     * Get paginated list of service tickets with filters.
     * 
     * @param page Page number (default: 0)
     * @param size Page size (default: 10)
     * @param date Filter by received date (optional)
     * @param status Filter by ticket status - case insensitive (optional: DRAFT, CREATED, IN_PROGRESS, COMPLETED, CANCELLED)
     * @param search Search by ticket code, customer name, phone, or license plate (optional)
     * @return Page of ServiceTicketListResponse
     */
    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<Page<ServiceTicketListResponse>>> getServiceTicketList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        
        // Parse status string to enum (case-insensitive)
        TicketStatus ticketStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                ticketStatus = TicketStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Invalid status value - ignore and return all
                ticketStatus = null;
            }
        }
        
        Page<ServiceTicketListResponse> tickets = serviceTicketManageService.getServiceTicketList(
            page, size, date, ticketStatus, search);
        
        return ResponseEntity.ok(ApiResponses.success(tickets));
    }
    
    /**
     * Get service ticket detail by ticket code.
     * 
     * @param ticketCode Ticket code (ST_XXXXXX or MST_XXXXXX)
     * @return ServiceTicketDetailResponse with full information
     */
    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> getServiceTicketDetail(
            @PathVariable String ticketCode) {
        
        ServiceTicketDetailResponse detail = serviceTicketManageService.getServiceTicketDetail(ticketCode);
        
        return ResponseEntity.ok(ApiResponses.success(detail));
    }
}
