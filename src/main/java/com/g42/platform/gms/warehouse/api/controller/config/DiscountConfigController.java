package com.g42.platform.gms.warehouse.api.controller.config;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.app.service.discount.DiscountService;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.infrastructure.entity.DiscountConfigJpa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/warehouse/discount-configs")
@RequiredArgsConstructor
public class DiscountConfigController {

    private final DiscountService discountService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<DiscountConfigJpa>> create(
            @Valid @RequestBody CreateDiscountConfigRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        DiscountConfigJpa saved = discountService.create(
                request.getItemId(),
                request.getIssueType(),
                request.getQuantityThreshold(),
                request.getDiscountRate(),
                principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(saved));
    }

    @Data
    public static class CreateDiscountConfigRequest {
        private Integer itemId;
        private IssueType issueType;
        private Integer quantityThreshold;
        @NotNull
        private BigDecimal discountRate;
    }
}
