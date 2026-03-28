package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceQueueResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketListResponse;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketManageService;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for receptionist view of service tickets.
 * Lễ tân: xem danh sách, xem chi tiết, xác nhận thanh toán (COMPLETED → PAID).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-ticket/manage")
public class ServiceTicketManageController {

    private final ServiceTicketManageService serviceTicketManageService;

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<Page<ServiceTicketListResponse>>> getServiceTicketList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        TicketStatus ticketStatus = null;
        if (status != null && !status.isBlank()) {
            try { ticketStatus = TicketStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(ApiResponses.success(
            serviceTicketManageService.getServiceTicketList(page, size, date, ticketStatus, search)));
    }

    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> getServiceTicketDetail(
            @PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(serviceTicketManageService.getServiceTicketDetail(ticketCode)));
    }

    /**
     * Lễ tân xác nhận thanh toán — COMPLETED → PAID, trigger ZNS feedback.
     * Endpoint này do bạn tôi implement phần thanh toán.
     */
    @PostMapping("/tickets/{ticketCode}/complete")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> completeTicket(
            @PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(serviceTicketManageService.completeTicket(ticketCode)));
    }

    /**
     * Lễ tân thay đổi advisor cho ticket.
     * Chỉ được phép thay đổi khi advisor hiện tại đang ở trạng thái PENDING.
     */
    @PutMapping("/tickets/{ticketCode}/change-advisor")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> changeAdvisor(
            @PathVariable String ticketCode,
            @RequestParam Integer newAdvisorId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(ApiResponses.success(
            serviceTicketManageService.changeAdvisor(ticketCode, newAdvisorId, note)));
    }
    @PutMapping("/swap")
    public ResponseEntity<ApiResponse<List<ServiceQueueResponse>>> swapQueue(@RequestParam Integer serviceTicketId1,
                                                                             @RequestParam Integer serviceTicketId2){
        return ResponseEntity.ok(ApiResponses.success(serviceTicketManageService.setswapQueueByServiceTicketIds(serviceTicketId1, serviceTicketId2)));
    }
}
