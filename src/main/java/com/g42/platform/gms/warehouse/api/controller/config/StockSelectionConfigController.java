package com.g42.platform.gms.warehouse.api.controller.config;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.StockSelectionConfigDto;
import com.g42.platform.gms.warehouse.api.dto.StockSuggestionDto;
import com.g42.platform.gms.warehouse.app.service.allocation.StockSelectionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Cấu hình kho mặc định / chiến lược chọn lô, và endpoint gợi ý kho + lô
 * cho ô tìm nhanh vật tư trên bảng báo giá.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/warehouse/stock-selection")
public class StockSelectionConfigController {

    private final StockSelectionService stockSelectionService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<StockSelectionConfigDto>> getConfig() {
        return ResponseEntity.ok(ApiResponses.success(stockSelectionService.getConfigDto()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<StockSelectionConfigDto>> saveConfig(
            @RequestBody StockSelectionConfigDto request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal != null ? principal.getStaffId() : null;
        return ResponseEntity.ok(ApiResponses.success(stockSelectionService.saveConfig(request, staffId)));
    }

    /** Gợi ý kho + lô cho một vật tư theo cấu hình đang có hiệu lực. */
    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<StockSuggestionDto>> suggest(
            @RequestParam Integer itemId,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) Integer warehouseId) {
        return ResponseEntity.ok(ApiResponses.success(
                stockSelectionService.suggest(itemId, quantity, warehouseId)));
    }
}
