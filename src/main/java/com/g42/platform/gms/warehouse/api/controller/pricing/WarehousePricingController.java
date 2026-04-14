package com.g42.platform.gms.warehouse.api.controller.pricing;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.request.UpsertPricingRequest;
import com.g42.platform.gms.warehouse.api.dto.response.PricingResponse;
import com.g42.platform.gms.warehouse.app.service.pricing.WarehousePricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/pricing")
@RequiredArgsConstructor
public class WarehousePricingController {

    private final WarehousePricingService pricingService;

    /** Danh sách giá thị trường đang active theo kho */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PricingResponse>>> list(
            @RequestParam Integer warehouseId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponses.success(
                pricingService.searchByWarehouse(warehouseId, isActive, search, page, size)));
    }

    /** Tạo hoặc cập nhật giá thị trường cho 1 item */
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PricingResponse>> upsert(
            @Valid @RequestBody UpsertPricingRequest request) {
        return ResponseEntity.ok(ApiResponses.success(pricingService.upsert(request)));
    }

    /** Deactivate giá (xóa mềm) */
    @DeleteMapping("/{pricingId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Integer pricingId) {
        pricingService.deactivate(pricingId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
