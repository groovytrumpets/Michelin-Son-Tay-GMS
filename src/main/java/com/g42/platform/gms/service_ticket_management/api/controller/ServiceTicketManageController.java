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
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.DuplicateCancelRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.manage.DuplicateCancelSummary;
import com.g42.platform.gms.service_ticket_management.application.service.DuplicateTicketCancelService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketAdvisorService;
import com.g42.platform.gms.service_ticket_management.application.service.ServiceTicketManageService;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final DuplicateTicketCancelService duplicateTicketCancelService;

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<Page<ServiceTicketListResponse>>> getServiceTicketList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String ticketType,
            // null = tất cả; true = chỉ phiếu bán lẻ khách vãng lai
            @RequestParam(required = false) Boolean walkIn,
            // null = mọi xưởng
            @RequestParam(required = false) Integer branchId) {

        TicketStatus ticketStatus = null;
        if (status != null && !status.isBlank()) {
            try { ticketStatus = TicketStatus.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        TicketType type = null;
        if (ticketType != null && !ticketType.isBlank()) {
            try { type = TicketType.valueOf(ticketType.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(ApiResponses.success(
            serviceTicketManageService.getServiceTicketList(page, size, date, ticketStatus, search, type, walkIn, branchId)));
    }

    @GetMapping("/tickets/{ticketCode}")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> getServiceTicketDetail(
            @PathVariable String ticketCode) {
        return ResponseEntity.ok(ApiResponses.success(serviceTicketManageService.getServiceTicketDetail(ticketCode)));
    }

    /**
     * Sửa xưởng ghi trên phiếu (và lịch hẹn gốc) khi lỡ tạo phiếu lúc máy đang chọn nhầm xưởng.
     * Không ảnh hưởng kho: kho dùng chung giữa các xưởng.
     */
    @PutMapping("/tickets/{ticketCode}/branch")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SERVICE_TICKET_EDIT + "')")
    @Auditable(action = "UPDATE", module = "SERVICE_TICKET", description = "Đổi xưởng của phiếu", targetType = "SERVICE_TICKET")
    public ResponseEntity<ApiResponse<ServiceTicketDetailResponse>> changeBranch(
            @PathVariable String ticketCode,
            @RequestParam Integer branchId) {
        return ResponseEntity.ok(ApiResponses.success(serviceTicketManageService.changeBranch(ticketCode, branchId)));
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

    /**
     * Xem trước việc huỷ phiếu trùng: hoá đơn nào bị huỷ, hàng nào trả về kho, lưu ý gì.
     * Không ghi gì. Chỉ ADMIN (mã quyền khoá cứng cho ADMIN qua PermissionCodes.ROLE_RESTRICTED).
     */
    @GetMapping("/{serviceTicketId}/cancel-duplicate/preview")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SERVICE_TICKET_CANCEL_DUPLICATE + "')")
    public ResponseEntity<ApiResponse<DuplicateCancelSummary>> previewCancelDuplicate(
            @PathVariable Integer serviceTicketId,
            @RequestParam(required = false) String duplicateOfTicketCode,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                duplicateTicketCancelService.preview(serviceTicketId, duplicateOfTicketCode, principal)));
    }

    /** Huỷ phiếu bị tạo trùng, kể cả đã hoàn thành / đã thanh toán. Chỉ ADMIN. */
    @PostMapping("/{serviceTicketId}/cancel-duplicate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.SERVICE_TICKET_CANCEL_DUPLICATE + "')")
    @Auditable(action = "DELETE", module = "SERVICE_TICKET", severity = "CRITICAL",
            description = "Huỷ phiếu dịch vụ trùng (huỷ hoá đơn, giao dịch thu tiền, hoàn kho)",
            targetType = "SERVICE_TICKET")
    public ResponseEntity<ApiResponse<DuplicateCancelSummary>> cancelDuplicate(
            @PathVariable Integer serviceTicketId,
            @RequestBody DuplicateCancelRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        DuplicateCancelSummary result = duplicateTicketCancelService.cancel(serviceTicketId,
                request.getDuplicateOfTicketCode(), request.getReason(), principal);
        return ResponseEntity.ok(ApiResponses.success(result, "Đã huỷ phiếu trùng " + result.getTicket().getTicketCode()));
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String ticketType){
        TicketType type = null;
        if (ticketType != null && !ticketType.isBlank()) {
            try { type = TicketType.valueOf(ticketType.toUpperCase()); } catch (IllegalArgumentException ignored) {}
        }
        byte [] excelConetnt = serviceTicketManageService.exportTicketToExcel(startDate, endDate, type);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment","Danh_Sach_Phieu_Dich_Vu.xlsx");

        return ResponseEntity.ok().headers(headers).body(excelConetnt);
    }
}
