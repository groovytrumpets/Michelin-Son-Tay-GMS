package com.g42.platform.gms.warehouse.api.controller.pricing;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.request.UpsertPricingRequest;
import com.g42.platform.gms.warehouse.api.dto.response.PricingResponse;
import com.g42.platform.gms.warehouse.app.service.pricing.WarehousePricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/pricing")
@RequiredArgsConstructor
public class WarehousePricingController {

    private final WarehousePricingService pricingService;

    /** Danh sách cấu hình giá theo kho */
    @GetMapping("/{warehouseId}")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<PricingResponse>>> list(
            @PathVariable Integer warehouseId) {
        return ResponseEntity.ok(ApiResponses.success(pricingService.listByWarehouse(warehouseId)));
    }

    /** Tạo hoặc cập nhật giá cho 1 item trong kho */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PricingResponse>> upsert(
            @Valid @RequestBody UpsertPricingRequest request) {
        return ResponseEntity.ok(ApiResponses.success(pricingService.upsert(request)));
    }

    /** Vô hiệu hóa 1 cấu hình giá */
    @DeleteMapping("/{pricingId}")
    @PreAuthorize("hasAnyAuthority('MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Integer pricingId) {
        pricingService.deactivate(pricingId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
