package com.g42.platform.gms.service_ticket_management.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.service_ticket_management.api.dto.parts_sale.PartsSaleCreateDto;
import com.g42.platform.gms.service_ticket_management.api.dto.parts_sale.PartsSaleTicketDto;
import com.g42.platform.gms.service_ticket_management.application.service.PartsSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API bán linh kiện cho đại lý/garage khác — phiếu dịch vụ rút gọn.
 *
 * Thứ tự gọi: /hold (lưu báo giá, giữ hàng) → POST rỗng (chốt, sinh hoá đơn)
 * → thu ngân thu tiền (xuất kho thật). Huỷ giữa chừng thì gọi /{id}/cancel.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service-ticket/parts-sale")
public class PartsSaleController {

    private final PartsSaleService partsSaleService;

    /** Giữ hàng cho báo giá và tạo phiếu HOLDING hiện ở màn quản lý phiếu bán. */
    @PostMapping("/hold")
    public ResponseEntity<ApiResponse<PartsSaleTicketDto>> holdPartsSale(
            @RequestBody PartsSaleCreateDto request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        PartsSaleTicketDto result = partsSaleService.holdPartsSale(request, principal.getStaffId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponses.success(result));
    }

    /** Chốt phiếu: archive báo giá + sinh hoá đơn chờ thu tiền. */
    @PostMapping
    public ResponseEntity<ApiResponse<PartsSaleTicketDto>> createPartsSale(
            @RequestBody PartsSaleCreateDto request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        PartsSaleTicketDto result = partsSaleService.createPartsSale(request, principal.getStaffId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponses.success(result));
    }

    /** Chốt phiếu đang giữ hàng đã tạo từ trước (mở lại từ màn quản lý phiếu bán). */
    @PostMapping("/{serviceTicketId}/checkout")
    public ResponseEntity<ApiResponse<PartsSaleTicketDto>> checkoutHoldingTicket(
            @PathVariable Integer serviceTicketId,
            @AuthenticationPrincipal StaffPrincipal principal) {
        PartsSaleTicketDto result = partsSaleService.checkoutHoldingTicket(serviceTicketId, principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    /** Huỷ phiếu đang giữ hàng và nhả hàng về kho. */
    @PostMapping("/{serviceTicketId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelPartsSale(
            @PathVariable Integer serviceTicketId,
            @AuthenticationPrincipal StaffPrincipal principal) {
        partsSaleService.cancelPartsSale(serviceTicketId, principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
