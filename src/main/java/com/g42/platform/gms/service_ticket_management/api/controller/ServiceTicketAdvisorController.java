package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketListResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.UpdateServiceTicketRequest;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketAdvisorService;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketManageService;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for advisor actions on service tickets.
 * Advisor quản lý luồng sửa chữa: xem danh sách, cập nhật báo giá, điều phối trạng thái.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-ticket/advisor")
public class ServiceTicketAdvisorController {

    private final ServiceTicketAdvisorService advisorService;
    private final ServiceTicketManageService manageService; // reuse read methods

    /** Xem danh sách phiếu (advisor cũng cần xem). */
    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<Page<ServiceTicketListResponse>>> getList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        TicketStatus ticketStatus = null;
        if (status != null && !status.isBlank()) {
            try { ticketStatus = TicketStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(ApiResponses.success(manageService.getServiceTicketList(page, size, date, ticketStatus, search)));
    }

    /** Xem chi tiết phiếu. */
    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> getDetail(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(manageService.getServiceTicketDetail(ticketCode)));
    }

    /** Cập nhật dịch vụ/báo giá khi ticket đang DRAFT. */
    @PutMapping("/tickets/{ticketCode}")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> updateEstimate(
            @PathVariable String ticketCode,
            @RequestBody @jakarta.validation.Valid UpdateServiceTicketRequest request) {
        return ResponseEntity.ok(ApiResponses.success(advisorService.updateEstimate(ticketCode, request)));
    }

    /** DRAFT → IN_PROGRESS: khách đồng ý, bắt đầu sửa. */
    @PostMapping("/tickets/{ticketCode}/start")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> startService(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(advisorService.startService(ticketCode)));
    }

    /** IN_PROGRESS → PENDING: chờ phụ tùng. */
    @PostMapping("/tickets/{ticketCode}/wait-parts")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> waitForParts(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(advisorService.waitForParts(ticketCode)));
    }

    /** PENDING → IN_PROGRESS: có phụ tùng, tiếp tục sửa. */
    @PostMapping("/tickets/{ticketCode}/resume")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> resumeWork(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(advisorService.resumeWork(ticketCode)));
    }

    /** PENDING/IN_PROGRESS → DRAFT: thêm dịch vụ mới vào báo giá. */
    @PostMapping("/tickets/{ticketCode}/back-to-draft")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> backToDraft(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(advisorService.backToDraft(ticketCode)));
    }

    /** DRAFT/PENDING/IN_PROGRESS → CANCELLED. */
    @PostMapping("/tickets/{ticketCode}/cancel")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> cancelTicket(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(advisorService.cancelTicket(ticketCode)));
    }
}
