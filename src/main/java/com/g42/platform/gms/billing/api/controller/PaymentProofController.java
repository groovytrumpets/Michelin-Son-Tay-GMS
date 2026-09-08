package com.g42.platform.gms.billing.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.billing.api.dto.PaymentProofCreateDto;
import com.g42.platform.gms.billing.api.dto.PaymentProofDto;
import com.g42.platform.gms.billing.app.service.PaymentProofService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Chứng từ thanh toán (ảnh/video) của một phiếu dịch vụ.
 *
 * <p>Dùng ở màn thanh toán ({@code /service-ticket/:code/receipt-payment-method})
 * và màn chi tiết phiếu ({@code /service-ticket-detail/:code}). FE upload file lên
 * Cloudinary trước rồi POST danh sách URL sang đây.
 */
@RestController
@RequestMapping("/api/payment/proofs")
@RequiredArgsConstructor
public class PaymentProofController {

    private final PaymentProofService paymentProofService;

    @GetMapping("/{serviceTicketId}")
    public ResponseEntity<ApiResponse<List<PaymentProofDto>>> list(@PathVariable Integer serviceTicketId) {
        return ResponseEntity.ok(ApiResponses.success(paymentProofService.list(serviceTicketId)));
    }

    @PostMapping("/{serviceTicketId}")
    public ResponseEntity<ApiResponse<List<PaymentProofDto>>> add(
            @PathVariable Integer serviceTicketId,
            @RequestBody PaymentProofCreateDto dto,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal != null ? principal.getStaffId() : null;
        return ResponseEntity.ok(ApiResponses.success(paymentProofService.add(serviceTicketId, dto, staffId)));
    }

    @DeleteMapping("/{serviceTicketId}/{paymentProofId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Integer serviceTicketId,
            @PathVariable Integer paymentProofId) {
        paymentProofService.delete(serviceTicketId, paymentProofId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá chứng từ."));
    }
}
