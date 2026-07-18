package com.g42.platform.gms.customer.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customer.api.dto.CustomerPointsHistoryDto;
import com.g42.platform.gms.customer.api.dto.CustomerRankingDto;
import com.g42.platform.gms.customer.application.service.CustomerRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API điểm tích lũy & hạng khách hàng.
 *
 * Admin:    /api/admin/customer/{id}/ranking
 * Customer: /api/customer/ranking  (lấy của chính mình qua JWT)
 */
@RestController
@RequiredArgsConstructor
public class CustomerRankingController {

    private final CustomerRankingService rankingService;

    // ─── Admin endpoints ───────────────────────────────────────────────────────

    @GetMapping("/api/admin/customer/{customerId}/ranking")
    public ResponseEntity<ApiResponse<CustomerRankingDto>> getAdminRanking(
            @PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(rankingService.getRanking(customerId)));
    }

    @PostMapping("/api/admin/customer/{customerId}/ranking/add-points")
    public ResponseEntity<ApiResponse<CustomerRankingDto>> addPointsForService(
            @PathVariable Integer customerId,
            @RequestParam long amountSpent,
            @RequestParam(required = false) Integer bookingId) {
        return ResponseEntity.ok(ApiResponses.success(
                rankingService.addPointsForService(customerId, amountSpent, bookingId)));
    }

    @PostMapping("/api/admin/customer/{customerId}/ranking/adjust")
    public ResponseEntity<ApiResponse<CustomerRankingDto>> adjustPoints(
            @PathVariable Integer customerId,
            @RequestParam int delta,
            @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponses.success(
                rankingService.adjustPoints(customerId, delta, reason)));
    }

    @GetMapping("/api/admin/customer/{customerId}/ranking/history")
    public ResponseEntity<ApiResponse<Page<CustomerPointsHistoryDto>>> getAdminHistory(
            @PathVariable Integer customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponses.success(rankingService.getHistory(customerId, page, size)));
    }

    // ─── Customer self-service endpoints ──────────────────────────────────────

    /**
     * Khách hàng xem điểm & hạng của chính mình.
     * customerId được lấy từ security context (xem note dưới).
     */
    @GetMapping("/api/customer/ranking")
    public ResponseEntity<ApiResponse<CustomerRankingDto>> getMyRanking(
            @RequestParam Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(rankingService.getRanking(customerId)));
    }

    @GetMapping("/api/customer/ranking/history")
    public ResponseEntity<ApiResponse<Page<CustomerPointsHistoryDto>>> getMyHistory(
            @RequestParam Integer customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponses.success(rankingService.getHistory(customerId, page, size)));
    }
}
