package com.g42.platform.gms.warehouse.api.controller.inventory;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.response.InventoryResponse;
import com.g42.platform.gms.warehouse.app.service.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Danh sách tồn kho theo kho — phân quyền field theo role:
     * - WAREHOUSE_KEEPER: chỉ thấy số lượng
     * - ADVISOR: thấy số lượng + giá bán
     * - ACCOUNTANT / MANAGER / ADMIN: thấy tất cả kể cả giá nhập
     */
    @GetMapping("/{warehouseId}")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','ADVISOR','ACCOUNTANT','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> listByWarehouse(
            @PathVariable Integer warehouseId,
            @AuthenticationPrincipal StaffPrincipal principal) {

        boolean showImportPrice = hasAnyRole(principal, "ACCOUNTANT", "MANAGER", "ADMIN");
        boolean showSellingPrice = hasAnyRole(principal, "ADVISOR", "ACCOUNTANT", "MANAGER", "ADMIN");

        List<InventoryResponse> result = inventoryService.listByWarehouse(
                warehouseId, showImportPrice, showSellingPrice);
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    /** Số lượng khả dụng đơn lẻ */
    @GetMapping("/{warehouseId}/{itemId}/available")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Integer>> getAvailableQuantity(
            @PathVariable Integer warehouseId,
            @PathVariable Integer itemId) {
        return ResponseEntity.ok(ApiResponses.success(
                inventoryService.getAvailableQuantity(warehouseId, itemId)));
    }

    /**
     * Tìm kiếm phụ tùng theo keyword, kèm tồn kho hiện tại.
     * Dùng cho màn hình nhập kho — hiển thị cả item chưa có trong kho (qty=0).
     * WAREHOUSE_KEEPER / MANAGER / ADMIN thấy importPrice.
     */
    @GetMapping("/{warehouseId}/search")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','MANAGER','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> search(
            @PathVariable Integer warehouseId,
            @RequestParam String keyword,
            @AuthenticationPrincipal StaffPrincipal principal) {
        boolean showImportPrice = hasAnyRole(principal, "ACCOUNTANT", "MANAGER", "ADMIN", "WAREHOUSE_KEEPER");
        return ResponseEntity.ok(ApiResponses.success(
                inventoryService.searchByWarehouse(warehouseId, keyword, showImportPrice)));
    }

    /**
     * Lấy toàn bộ PART kèm tồn kho — dùng cho màn hình tổng quan nhập kho.
     * Item chưa có trong kho sẽ có quantity=0.
     */
    @GetMapping("/{warehouseId}/parts")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','MANAGER','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> listAllParts(
            @PathVariable Integer warehouseId,
            @AuthenticationPrincipal StaffPrincipal principal) {
        boolean showImportPrice = hasAnyRole(principal, "ACCOUNTANT", "MANAGER", "ADMIN", "WAREHOUSE_KEEPER");
        return ResponseEntity.ok(ApiResponses.success(
                inventoryService.listAllPartsWithInventory(warehouseId, showImportPrice)));
    }

    private boolean hasAnyRole(StaffPrincipal principal, String... roles) {
        if (principal == null) return false;
        return principal.getAuthorities().stream()
                .anyMatch(a -> {
                    for (String role : roles) {
                        // authorities có prefix ROLE_
                        if (a.getAuthority().equals("ROLE_" + role)) return true;
                    }
                    return false;
                });
    }
}
