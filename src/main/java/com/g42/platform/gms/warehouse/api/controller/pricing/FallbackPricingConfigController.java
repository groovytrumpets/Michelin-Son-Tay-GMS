package com.g42.platform.gms.warehouse.api.controller.pricing;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.request.UpsertFallbackPricingRequest;
import com.g42.platform.gms.warehouse.api.dto.response.FallbackPricingResponse;
import com.g42.platform.gms.warehouse.app.service.pricing.FallbackPricingConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouse/fallback-pricing")
@RequiredArgsConstructor
public class FallbackPricingConfigController {

    private final FallbackPricingConfigService fallbackService;

    /** Danh sách cấu hình markup mặc định / fallback */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<FallbackPricingResponse>>> list(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponses.success(
                fallbackService.search(isActive, search, page, size)));
    }

    /** Tạo cấu hình markup fallback mới */
    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRICING_EDIT + "')")
    public ResponseEntity<ApiResponse<FallbackPricingResponse>> create(
            @Valid @RequestBody UpsertFallbackPricingRequest request) {
        return ResponseEntity.ok(ApiResponses.success(fallbackService.create(request)));
    }

    /** Cập nhật cấu hình markup fallback */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRICING_EDIT + "')")
    public ResponseEntity<ApiResponse<FallbackPricingResponse>> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpsertFallbackPricingRequest request) {
        return ResponseEntity.ok(ApiResponses.success(fallbackService.update(id, request)));
    }

    /** Xóa/Deactivate cấu hình markup fallback */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRICING_EDIT + "')")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Integer id) {
        fallbackService.deactivate(id);
        return ResponseEntity.ok(ApiResponses.success(null));
    }

    /** Kích hoạt cấu hình markup fallback */
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.PRICING_EDIT + "')")
    public ResponseEntity<ApiResponse<Void>> activate(@PathVariable Integer id) {
        fallbackService.activate(id);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
