package com.g42.platform.gms.warehouse.app.service.inventory;

import com.g42.platform.gms.common.service.ExcelService;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.StockEntry;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Luồng sync Excel giữa GMS và hệ thống kho T3.
 *
 * Format Excel đồng nhất (cả import lẫn export) — hỗ trợ nhiều lô per item:
 * | STT | SKU | Tên phụ tùng | Đơn vị | Mã lô | Ngày nhập | Tồn lô | Giá nhập (VNĐ) | Hệ số markup | Ghi chú |
 *
 * Mỗi dòng = 1 lô của 1 item. Cùng SKU có thể có nhiều dòng (nhiều lô).
 *
 * Import (T3 → GMS):
 *   - Tạo 1 stock_entry SYNC CONFIRMED per lần sync
 *   - Mỗi dòng → 1 stock_entry_item (1 lô) với giá nhập riêng
 *   - inventory.quantity = tổng tất cả lô của item đó trong file
 *   - Lô cũ trong GMS bị xóa (invalidate) trước khi insert lô mới từ T3
 *
 * Export (GMS → T3):
 *   - Xuất từng lô còn hàng (remainingQuantity > 0) theo từng item
 *   - Cùng format để T3 cập nhật lại kho của họ
 */
@Service
@RequiredArgsConstructor
public class InventoryExcelService {

    private final PartCatalogRepo partCatalogRepo;
    private final InventoryRepo inventoryRepo;
    private final StockEntryRepo stockEntryRepo;

    // Cột Excel (0-indexed) — format có lô
    private static final int COL_STT       = 0;
    private static final int COL_SKU       = 1;
    private static final int COL_NAME      = 2;
    private static final int COL_UNIT      = 3;
    private static final int COL_LOT_CODE  = 4;  // Mã lô (từ T3)
    private static final int COL_LOT_DATE  = 5;  // Ngày nhập lô
    private static final int COL_QTY       = 6;  // Tồn lô này
    private static final int COL_PRICE     = 7;  // Giá nhập lô này
    private static final int COL_MARKUP    = 8;
    private static final int COL_NOTE      = 9;

    static final String[] HEADERS = {
            "STT", "SKU", "Tên phụ tùng", "Đơn vị",
            "Mã lô", "Ngày nhập", "Tồn lô",
            "Giá nhập (VNĐ)", "Hệ số markup", "Ghi chú"
    };

    // ── EXPORT (GMS → T3) ────────────────────────────────────────────────────

    /**
     * Xuất từng lô còn hàng trong GMS ra Excel — cùng format để T3 cập nhật lại.
     * Mỗi dòng = 1 lô của 1 item (cùng SKU có thể có nhiều dòng).
     * GET /api/warehouse/inventory/{warehouseId}/excel/sync-template
     */
    public byte[] exportForSync(Integer warehouseId) {
        // Lấy tất cả lô còn hàng trong kho
        List<StockEntryItem> activeLots = stockEntryRepo.findActiveLotsByWarehouse(warehouseId);

        // Build map itemId → catalog item
        List<Integer> itemIds = activeLots.stream().map(StockEntryItem::getItemId).distinct().toList();
        Map<Integer, CatalogItem> catalogMap = itemIds.isEmpty()
                ? Map.of()
            : partCatalogRepo.findAllPartsByIds(itemIds).stream()
                .collect(Collectors.toMap(CatalogItem::getItemId, c -> c));

        // Build map entryId → entry (để lấy entryCode và entryDate)
        Map<Integer, StockEntry> entryMap = activeLots.stream()
                .map(StockEntryItem::getEntryId)
                .distinct()
                .map(id -> stockEntryRepo.findEntryById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(StockEntry::getEntryId, e -> e));

        int[] stt = {1};
        return ExcelService.exportToExcel(activeLots, HEADERS, lot -> {
            CatalogItem cat = catalogMap.get(lot.getItemId());
            StockEntry entry = entryMap.get(lot.getEntryId());
            return new Object[]{
                    stt[0]++,
                    cat != null && cat.getSku() != null ? cat.getSku() : "",
                    cat != null ? cat.getItemName() : "",
                    cat != null && cat.getUnit() != null ? cat.getUnit() : "",
                    entry != null ? entry.getEntryCode() : "",          // Mã lô
                    entry != null && entry.getEntryDate() != null
                            ? entry.getEntryDate().toString() : "",     // Ngày nhập
                    lot.getRemainingQuantity(),                          // Tồn lô
                    lot.getImportPrice(),                                // Giá nhập
                    lot.getMarkupMultiplier(),
                    lot.getNotes() != null ? lot.getNotes() : ""
            };
        });
    }

    // ── IMPORT (T3 → GMS) ────────────────────────────────────────────────────

    /**
     * Sync toàn bộ tồn kho từ file Excel của T3.
     * Mỗi dòng = 1 lô của 1 item. Cùng SKU có thể có nhiều dòng.
     * Logic:
     *   1. Xóa tất cả lô SYNC cũ của warehouse này (invalidate lô cũ từ T3)
     *   2. Tạo 1 stock_entry SYNC mới
     *   3. Mỗi dòng → 1 stock_entry_item (lô riêng với giá riêng)
     *   4. inventory.quantity = tổng remainingQuantity của tất cả lô item đó trong file
     * POST /api/warehouse/inventory/{warehouseId}/excel/sync
     */
    @Transactional
    public SyncResult syncFromT3Excel(MultipartFile file, Integer warehouseId, Integer staffId) {

        // Build map SKU → catalog item
        Map<String, CatalogItem> skuToCatalog = partCatalogRepo.findAllParts().stream()
                .filter(p -> p.getSku() != null)
                .collect(Collectors.toMap(
                        p -> p.getSku().trim().toLowerCase(),
                        p -> p,
                        (a, b) -> a
                ));

        List<Row> rows = ExcelService.importFromExcel(file, row -> row);
        List<String> errors = new ArrayList<>();

        // Bước 1: Invalidate tất cả lô SYNC cũ của warehouse này
        // (đặt remainingQuantity = 0 để FIFO không dùng nữa)
        stockEntryRepo.invalidateSyncLotsByWarehouse(warehouseId);

        // Bước 2: Tạo stock_entry SYNC mới
        StockEntry syncEntry = buildSyncEntry(warehouseId, staffId);
        StockEntry savedEntry = stockEntryRepo.save(syncEntry);

        // Bước 3: Parse từng dòng → tích lũy qty per item + tạo lô
        Map<Integer, Integer> itemTotalQty = new LinkedHashMap<>(); // itemId → tổng qty từ file

        for (Row row : rows) {
            int rowNum = row.getRowNum() + 1;

            String sku = getCellString(row, COL_SKU);
            if (sku == null || sku.isBlank()) continue;

            String itemName = getCellString(row, COL_NAME);
            if (itemName == null || itemName.isBlank()) {
                errors.add("Dòng " + rowNum + ": Thiếu tên phụ tùng (SKU=" + sku + ")");
                continue;
            }

            Integer qty = getCellInt(row, COL_QTY);
            if (qty == null || qty < 0) {
                errors.add("Dòng " + rowNum + ": Số lượng không hợp lệ (SKU=" + sku + ")");
                continue;
            }

            BigDecimal importPrice = getCellDecimal(row, COL_PRICE);
            if (importPrice == null) importPrice = BigDecimal.ZERO;

            BigDecimal markup = getCellDecimal(row, COL_MARKUP);
            if (markup == null || markup.compareTo(BigDecimal.ZERO) <= 0) markup = new BigDecimal("1.3");

            String unit    = getCellString(row, COL_UNIT);
            String lotCode = getCellString(row, COL_LOT_CODE);
            String notes   = lotCode != null ? "Lô " + lotCode : getCellString(row, COL_NOTE);

            // Tìm catalog item theo SKU — tạo mới nếu chưa có (item từ T3)
            CatalogItem catalogItem = skuToCatalog.get(sku.trim().toLowerCase());
            if (catalogItem == null) {
                catalogItem = createDefaultCatalogItem(sku.trim(), itemName, unit);
                catalogItem = partCatalogRepo.save(catalogItem);
                skuToCatalog.put(sku.trim().toLowerCase(), catalogItem);
            } else {
                // Cập nhật unit nếu T3 có thay đổi
                if (unit != null && !unit.isBlank() && !unit.equals(catalogItem.getUnit())) {
                    catalogItem.setUnit(unit);
                    partCatalogRepo.save(catalogItem);
                }
            }

            Integer itemId = catalogItem.getItemId();

            // Tích lũy tổng qty per item
            itemTotalQty.merge(itemId, qty, Integer::sum);

            // Tạo stock_entry_item cho lô này (chỉ khi qty > 0 và có giá)
            if (qty > 0 && importPrice.compareTo(BigDecimal.ZERO) > 0) {
                StockEntryItem entryItem = StockEntryItem.builder()
                        .entryId(savedEntry.getEntryId())
                        .itemId(itemId)
                        .quantity(qty)
                        .importPrice(importPrice)
                        .markupMultiplier(markup)
                        .remainingQuantity(qty)
                        .notes(notes)
                        .build();
                stockEntryRepo.saveItem(entryItem);
            }
        }

        // Bước 4: Upsert inventory — set quantity = tổng tất cả lô của item trong file
        int inventoryUpdated = 0, inventoryInserted = 0;
        for (Map.Entry<Integer, Integer> e : itemTotalQty.entrySet()) {
            Integer itemId = e.getKey();
            Integer totalQty = e.getValue();
            Inventory inv = inventoryRepo.findByWarehouseAndItem(warehouseId, itemId).orElse(null);
            if (inv != null) {
                inv.setQuantity(totalQty);
                inventoryRepo.save(inv);
                inventoryUpdated++;
            } else {
                Inventory newInv = Inventory.builder()
                        .warehouseId(warehouseId)
                        .itemId(itemId)
                        .quantity(totalQty)
                        .reservedQuantity(0)
                        .build();
                inventoryRepo.save(newInv);
                inventoryInserted++;
            }
        }

        return new SyncResult(savedEntry.getEntryId(), inventoryUpdated, inventoryInserted, errors);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private StockEntry buildSyncEntry(Integer warehouseId, Integer staffId) {
        return StockEntry.builder()
                .entryCode("SYNC-" + warehouseId + "-" + System.currentTimeMillis())
                .warehouseId(warehouseId)
                .supplierName("SYNC - Đồng bộ từ kho T3")
                .entryDate(LocalDate.now())
                .status(StockEntryStatus.CONFIRMED)
                .notes("Đồng bộ tồn kho từ hệ thống kho T3")
                .createdBy(staffId)
                .confirmedBy(staffId)
                .confirmedAt(LocalDateTime.now())
                .build();
    }

    private Map<Integer, BigDecimal> buildLatestPriceMap(Integer warehouseId) {
        return Map.of();
    }

    private Map<Integer, BigDecimal> buildLatestMarkupMap(Integer warehouseId) {
        return Map.of();
    }

    /**
     * Tạo catalog item mới từ T3 — brand/product_line/work_category để null.
     * Manager sẽ phân loại sau qua admin.
     */
    private CatalogItem createDefaultCatalogItem(String sku, String itemName, String unit) {
        CatalogItem item = new CatalogItem();
        item.setSku(sku);
        item.setItemName(itemName);
        item.setItemType(com.g42.platform.gms.warehouse.domain.enums.CatalogItemType.PART);
        item.setUnit(unit);
        item.setIsActive(true);
        // brand_id, product_line_id, work_category_id để null — manager cập nhật sau
        return item;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return null;
    }

    private Integer getCellInt(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return (int) Math.round(cell.getNumericCellValue());
        if (cell.getCellType() == CellType.STRING) {
            try { return (int) Math.round(Double.parseDouble(cell.getStringCellValue().trim())); }
            catch (Exception e) { return null; }
        }
        return null;
    }

    private BigDecimal getCellDecimal(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        if (cell.getCellType() == CellType.STRING) {
            String s = cell.getStringCellValue().trim();
            if (s.isEmpty()) return null;
            try { return new BigDecimal(s); } catch (Exception e) { return null; }
        }
        return null;
    }

    public record SyncResult(
            Integer syncEntryId,
            int inventoryUpdated,
            int inventoryInserted,
            List<String> errors
    ) {}
}
