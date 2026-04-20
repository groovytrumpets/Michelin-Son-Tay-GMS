package com.g42.platform.gms.warehouse.api.controller.inventory;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.response.InventoryResponse;
import com.g42.platform.gms.warehouse.app.service.inventory.InventoryExcelService;
import com.g42.platform.gms.warehouse.app.service.inventory.InventoryService;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouse/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryExcelService inventoryExcelService;
    private final StockEntryRepo stockEntryRepo;

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

    /**
     * Xuất tồn kho ra Excel.
     * GET /api/warehouse/inventory/{warehouseId}/export
     */
    @GetMapping("/{warehouseId}/export")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','MANAGER','ADMIN','ACCOUNTANT')")
    public ResponseEntity<byte[]> exportInventory(
            @PathVariable Integer warehouseId,
            @AuthenticationPrincipal StaffPrincipal principal) {
        boolean showImportPrice = hasAnyRole(principal, "ACCOUNTANT", "MANAGER", "ADMIN");
        boolean showSellingPrice = hasAnyRole(principal, "ADVISOR", "ACCOUNTANT", "MANAGER", "ADMIN", "WAREHOUSE_KEEPER");

        List<InventoryResponse> data = inventoryService.listByWarehouse(warehouseId, showImportPrice, showSellingPrice);

        String[] headers = {"STT", "Item ID", "Tên phụ tùng", "SKU", "Đơn vị",
                "Tồn kho", "Đang giữ", "Khả dụng", "Giá bán", "Giá nhập"};
        int[] stt = {1};
        byte[] bytes = com.g42.platform.gms.common.service.ExcelService.exportToExcel(data, headers, inv -> new Object[]{
                stt[0]++,
                inv.getItemId(),
                inv.getItemName() != null ? inv.getItemName() : "",
                inv.getSku() != null ? inv.getSku() : "",
                inv.getUnit() != null ? inv.getUnit() : "",
                inv.getQuantity(),
                inv.getReservedQuantity(),
                inv.getAvailableQuantity(),
                inv.getSellingPrice() != null ? inv.getSellingPrice() : "",
                inv.getImportPrice() != null ? inv.getImportPrice() : ""
        });

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"inventory-warehouse-" + warehouseId + ".xlsx\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * Export tồn kho ra Excel để chỉnh sửa rồi import lại (sync mode).
     * GET /api/warehouse/inventory/{warehouseId}/excel/sync-template
     */
    @GetMapping("/{warehouseId}/excel/sync-template")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','MANAGER','ADMIN')")
    public ResponseEntity<byte[]> exportForSync(@PathVariable Integer warehouseId) {
        byte[] bytes = inventoryExcelService.exportForSync(warehouseId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"inventory-sync-" + warehouseId + ".xlsx\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * Import/upsert tồn kho từ Excel của T3 (full sync snapshot).
     * Upsert catalog_item + inventory + stock_entry_item (FIFO).
     * POST /api/warehouse/inventory/{warehouseId}/excel/sync
     */
    @PostMapping(value = "/{warehouseId}/excel/sync", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> syncInventory(
            @PathVariable Integer warehouseId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal StaffPrincipal principal) {
        InventoryExcelService.SyncResult result =
                inventoryExcelService.syncFromT3Excel(file, warehouseId, principal.getStaffId());
        java.util.Map<String, Object> resp = java.util.Map.of(
                "syncEntryId", result.syncEntryId(),
                "inventoryUpdated", result.inventoryUpdated(),
                "inventoryInserted", result.inventoryInserted(),
                "errors", result.errors(),
                "hasErrors", !result.errors().isEmpty()
        );
        return ResponseEntity.ok(ApiResponses.success(resp));
    }

    /**
     * Xem danh sách lô FIFO của 1 item trong kho.
     * Hiển thị: lô nào còn hàng, giá nhập, markup, số lượng còn lại.
     * GET /api/warehouse/inventory/{warehouseId}/{itemId}/lots
     */
    @GetMapping("/{warehouseId}/{itemId}/lots")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','MANAGER','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLots(
            @PathVariable Integer warehouseId,
            @PathVariable Integer itemId) {

        List<StockEntryItem> lots = stockEntryRepo.findFifoLots(warehouseId, itemId);
        List<Map<String, Object>> result = lots.stream().map(lot -> {
            var entry = stockEntryRepo.findEntryById(lot.getEntryId()).orElse(null);
            return Map.<String, Object>of(
                    "entryItemId",        lot.getEntryItemId(),
                    "entryId",            lot.getEntryId(),
                    "entryCode",          entry != null ? entry.getEntryCode() : "",
                    "entryDate",          entry != null && entry.getEntryDate() != null ? entry.getEntryDate().toString() : "",
                    "supplierName",       entry != null && entry.getSupplierName() != null ? entry.getSupplierName() : "",
                    "quantity",           lot.getQuantity(),
                    "remainingQuantity",  lot.getRemainingQuantity(),
                    "importPrice",        lot.getImportPrice(),
                    "markupMultiplier",   lot.getMarkupMultiplier(),
                    "notes",              lot.getNotes() != null ? lot.getNotes() : ""
            );
        }).toList();

        return ResponseEntity.ok(ApiResponses.success(result));
    }

    /**
     * Xem tất cả lô còn hàng trong kho (tất cả item).
     * Dùng để kiểm tra tổng quan FIFO.
     * GET /api/warehouse/inventory/{warehouseId}/lots
     */
    @GetMapping("/{warehouseId}/lots")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','MANAGER','ADMIN','ACCOUNTANT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllLots(
            @PathVariable Integer warehouseId) {

        List<StockEntryItem> lots = stockEntryRepo.findActiveLotsByWarehouse(warehouseId);
        List<Map<String, Object>> result = lots.stream().map(lot -> {
            var entry = stockEntryRepo.findEntryById(lot.getEntryId()).orElse(null);
            return Map.<String, Object>of(
                    "entryItemId",        lot.getEntryItemId(),
                    "entryId",            lot.getEntryId(),
                    "entryCode",          entry != null ? entry.getEntryCode() : "",
                    "entryDate",          entry != null && entry.getEntryDate() != null ? entry.getEntryDate().toString() : "",
                    "itemId",             lot.getItemId(),
                    "quantity",           lot.getQuantity(),
                    "remainingQuantity",  lot.getRemainingQuantity(),
                    "importPrice",        lot.getImportPrice(),
                    "markupMultiplier",   lot.getMarkupMultiplier()
            );
        }).toList();

        return ResponseEntity.ok(ApiResponses.success(result));
    }

    private boolean hasAnyRole(StaffPrincipal principal, String... roles) {
        if (principal == null) return false;
        return principal.getAuthorities().stream()
                .anyMatch(a -> {
                    for (String role : roles) {
                        if (a.getAuthority().equals("ROLE_" + role)) return true;
                    }
                    return false;
                });
    }
}
