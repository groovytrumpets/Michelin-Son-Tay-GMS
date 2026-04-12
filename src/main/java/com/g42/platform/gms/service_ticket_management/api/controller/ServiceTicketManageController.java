package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceQueueResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketDetailResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.ServiceTicketListResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.UpdateServiceTicketRequest;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketAdvisorService;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketManageService;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final ServiceTicketAdvisorService serviceTicketAdvisorService;

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
     * Endpoint này đã được thay thế bởi luồng billing.
     * Khi billing thanh toán xong sẽ tự động chuyển PAID + assignment DONE.
     * Giữ lại để backward compatibility, không expose ra nữa.
     */
    // @PostMapping("/tickets/{ticketCode}/complete") -- disabled, use billing flow instead

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
    @PutMapping("/{serviceTicketId}/{status}")
    public ResponseEntity<ApiResponse<ServiceTicketListResponse>> updateEstimateApprove(@PathVariable Integer serviceTicketId, @PathVariable TicketStatus status){
        return ResponseEntity.ok(
                ApiResponses.success(serviceTicketManageService.updateServiceTicketStatus(serviceTicketId,status))
        );
    }

    /** Lễ tân chỉnh sửa yêu cầu khách hàng và danh sách dịch vụ. */
    @PutMapping("/tickets/{ticketCode}")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> editServiceTicket(
            @PathVariable String ticketCode,
            @RequestBody UpdateServiceTicketRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                serviceTicketAdvisorService.updateEstimate(ticketCode, request)));
    }

    /** Advisor/lễ tân đặt thời gian hẹn lấy xe dự kiến */
    @PutMapping("/tickets/{ticketCode}/estimated-delivery")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> setEstimatedDelivery(
            @PathVariable String ticketCode,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime estimatedDeliveryAt) {
        return ResponseEntity.ok(ApiResponses.success(
                serviceTicketManageService.setEstimatedDelivery(ticketCode, estimatedDeliveryAt)));
    }

    /** Lễ tân xác nhận khách đã lấy xe thật sự — update delivered_at = now() */
    @PostMapping("/tickets/{ticketCode}/confirm-delivered")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> confirmDelivered(
            @PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(
                serviceTicketManageService.confirmDelivered(ticketCode)));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportServiceTicketList(            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate){
        byte [] excelConetnt = serviceTicketManageService.exportTicketToExcel(startDate, endDate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment","Danh_Sach_Phieu_Dich_Vu.xlsx");

        return ResponseEntity.ok().headers(headers).body(excelConetnt);
    }
}
