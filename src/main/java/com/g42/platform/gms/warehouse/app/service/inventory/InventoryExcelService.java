package com.g42.platform.gms.warehouse.app.service.inventory;


import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.common.service.ExcelService;
import com.g42.platform.gms.warehouse.domain.entity.*;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
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
 * Luồng nhập thêm tồn kho và thông tin sản phẩm từ file Excel.
 *
 * Format Excel đồng nhất (cả import lẫn export) — hỗ trợ đầy đủ thông tin:
 * | STT | SKU | Tên phụ tùng | Hạng mục | Hãng sản xuất | Dòng sản phẩm | Đơn vị tính | Giá bán | Hiển thị giá | Bảo hành | Xuất xứ | Màu sắc | Xe tương thích | Mô tả | Thuế | Mã lô | Ngày nhập | Tồn lô | Giá nhập | Hệ số markup | Ghi chú | Tổng tồn kho |
 */
@Service
@RequiredArgsConstructor
public class InventoryExcelService {

    private final PartCatalogRepo partCatalogRepo;
    private final InventoryRepo inventoryRepo;
    private final StockEntryRepo stockEntryRepo;
    private final CatalogItemRepo catalogItemRepo;
    private final com.g42.platform.gms.warehouse.app.service.catalog.ItemCategoryService itemCategoryService;
    private final InventoryTransactionRepo inventoryTransactionRepo;
    private final com.g42.platform.gms.estimation.api.internal.TaxRuleInternalApi taxRuleInternalApi;
    private final com.g42.platform.gms.estimation.infrastructure.repository.TaxRuleRepositoryJpa taxRuleRepositoryJpa;

    // Cột Excel (0-indexed) — format đầy đủ thông tin sản phẩm và lô
    private static final int COL_STT          = 0;
    private static final int COL_SKU          = 1;
    private static final int COL_NAME         = 2;
    private static final int COL_CATEGORY     = 3;
    private static final int COL_BRAND        = 4;
    private static final int COL_PRODUCT_LINE = 5;
    private static final int COL_UNIT         = 6;
    private static final int COL_PRICE_SELL   = 7;
    private static final int COL_SHOW_PRICE   = 8;
    private static final int COL_WARRANTY     = 9;
    private static final int COL_ORIGIN       = 10;
    private static final int COL_COLOR        = 11;
    private static final int COL_COMPATIBLE   = 12;
    private static final int COL_DESCRIPTION  = 13;
    private static final int COL_TAX          = 14;
    private static final int COL_LOT_CODE     = 15;
    private static final int COL_LOT_DATE     = 16;
    private static final int COL_QTY          = 17;
    private static final int COL_PRICE_BUY    = 18;
    private static final int COL_MARKUP       = 19;
    private static final int COL_NOTE         = 20;
    private static final int COL_TOTAL_STOCK  = 21;

    static final String[] HEADERS = {
            "STT", "SKU", "Tên phụ tùng", "Hạng mục", "Hãng sản xuất", "Dòng sản phẩm", "Đơn vị tính",
            "Giá bán (VNĐ)", "Hiển thị giá", "Bảo hành (tháng)", "Xuất xứ", "Màu sắc", "Xe tương thích",
            "Mô tả", "Thuế", "Mã lô", "Ngày nhập", "Tồn lô",
            "Giá nhập (VNĐ)", "Hệ số markup", "Ghi chú", "Tổng tồn kho"
    };

    // ── EXPORT (GMS → Excel) ──────────────────────────────────────────────────

    public byte[] exportForSync(Integer warehouseId) {
        // 1. Lấy tất cả CatalogItem có dạng PART
        List<CatalogItem> allParts = partCatalogRepo.findAllParts();

        // 2. Lấy tất cả lô còn hàng trong kho
        List<StockEntryItem> activeLots = stockEntryRepo.findActiveLotsByWarehouse(warehouseId);
        Map<Integer, List<StockEntryItem>> lotsByItemId = activeLots.stream()
                .collect(Collectors.groupingBy(StockEntryItem::getItemId));

        // 3. Lấy tất cả inventory trong kho
        List<Inventory> allInventory = inventoryRepo.findByWarehouse(warehouseId);
        Map<Integer, Inventory> inventoryMap = allInventory.stream()
                .collect(Collectors.toMap(Inventory::getItemId, i -> i, (a, b) -> a));

        // 4. Lấy map thông tin master data
        List<Brand> brands = catalogItemRepo.getAllBrands();
        Map<Integer, String> brandMap = brands.stream()
                .filter(b -> b.getBrandId() != null)
                .collect(Collectors.toMap(Brand::getBrandId, Brand::getBrandName, (a, b) -> a));

        List<ProductLine> lines = catalogItemRepo.getAllProductLines();
        Map<Integer, String> lineMap = lines.stream()
                .filter(l -> l.getProductLineId() != null)
                .collect(Collectors.toMap(ProductLine::getProductLineId, ProductLine::getLineName, (a, b) -> a));

        List<ItemCategory> categories = catalogItemRepo.getAllItemCategory();
        Map<Integer, String> categoryMap = categories.stream()
                .filter(c -> c.getItemCategoryId() != null)
                .collect(Collectors.toMap(ItemCategory::getItemCategoryId, ItemCategory::getCategoryName, (a, b) -> a));

        // 5. Build map entryId -> entry (lô)
        Map<Integer, StockEntry> entryMap = activeLots.stream()
                .map(StockEntryItem::getEntryId)
                .distinct()
                .map(id -> stockEntryRepo.findEntryById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(StockEntry::getEntryId, e -> e, (a, b) -> a));

        // 6. Xây dựng danh sách dòng Excel
        List<Object[]> rowsData = new ArrayList<>();
        int stt = 1;

        for (CatalogItem cat : allParts) {
            Integer itemId = cat.getItemId();
            List<StockEntryItem> lots = lotsByItemId.getOrDefault(itemId, Collections.emptyList());
            Inventory inv = inventoryMap.get(itemId);
            BigDecimal totalStock = inv != null ? Qty.nz(inv.getQuantity()) : BigDecimal.ZERO;

            String categoryName = cat.getItemCategoryId() != null ? categoryMap.get(cat.getItemCategoryId()) : "";
            String brandName = cat.getBrandId() != null ? brandMap.get(cat.getBrandId()) : "";
            String lineName = cat.getProductLineId() != null ? lineMap.get(cat.getProductLineId()) : "";

            String taxName = "";
            if (cat.getTaxRuleId() != null) {
                try {
                    com.g42.platform.gms.estimation.domain.entity.TaxRule tr = taxRuleInternalApi.getTaxRuleById(cat.getTaxRuleId());
                    if (tr != null) {
                        taxName = tr.getTaxName() != null ? tr.getTaxName() : (tr.getTaxRate() != null ? tr.getTaxRate().multiply(BigDecimal.valueOf(100)) + "%" : "");
                    }
                } catch (Exception ignored) {}
            }

            if (lots.isEmpty()) {
                rowsData.add(new Object[]{
                        stt++,
                        cat.getSku() != null ? cat.getSku() : "",
                        cat.getItemName() != null ? cat.getItemName() : "",
                        categoryName != null ? categoryName : "",
                        brandName != null ? brandName : "",
                        lineName != null ? lineName : "",
                        cat.getUnit() != null ? cat.getUnit() : "",
                        cat.getPrice() != null ? cat.getPrice() : BigDecimal.ZERO,
                        cat.getShowPrice() != null && cat.getShowPrice() ? "Có" : "Không",
                        cat.getWarrantyDurationMonths() != null ? cat.getWarrantyDurationMonths() : 0,
                        cat.getMadeIn() != null ? cat.getMadeIn() : "",
                        cat.getColor() != null ? cat.getColor() : "",
                        cat.getCompatibleCars() != null ? cat.getCompatibleCars() : "",
                        cat.getDescription() != null ? cat.getDescription() : "",
                        taxName,
                        "", // Mã lô
                        "", // Ngày nhập
                        0,  // Tồn lô
                        "", // Giá nhập
                        "", // Hệ số markup
                        "", // Ghi chú
                        totalStock
                });
            } else {
                for (StockEntryItem lot : lots) {
                    StockEntry entry = entryMap.get(lot.getEntryId());
                    rowsData.add(new Object[]{
                            stt++,
                            cat.getSku() != null ? cat.getSku() : "",
                            cat.getItemName() != null ? cat.getItemName() : "",
                            categoryName != null ? categoryName : "",
                            brandName != null ? brandName : "",
                            lineName != null ? lineName : "",
                            cat.getUnit() != null ? cat.getUnit() : "",
                            cat.getPrice() != null ? cat.getPrice() : BigDecimal.ZERO,
                            cat.getShowPrice() != null && cat.getShowPrice() ? "Có" : "Không",
                            cat.getWarrantyDurationMonths() != null ? cat.getWarrantyDurationMonths() : 0,
                            cat.getMadeIn() != null ? cat.getMadeIn() : "",
                            cat.getColor() != null ? cat.getColor() : "",
                            cat.getCompatibleCars() != null ? cat.getCompatibleCars() : "",
                            cat.getDescription() != null ? cat.getDescription() : "",
                            taxName,
                            entry != null ? entry.getEntryCode() : "",
                            entry != null && entry.getEntryDate() != null ? entry.getEntryDate().toString() : "",
                            lot.getRemainingQuantity(),
                            lot.getImportPrice() != null ? lot.getImportPrice() : BigDecimal.ZERO,
                            lot.getMarkupMultiplier() != null ? lot.getMarkupMultiplier() : BigDecimal.valueOf(1.3),
                            lot.getNotes() != null ? lot.getNotes() : "",
                            totalStock
                    });
                }
            }
        }

        return ExcelService.exportToExcel(rowsData, HEADERS, row -> row);
    }

    // ── IMPORT (Excel → GMS) ──────────────────────────────────────────────────

    @Transactional
    public SyncResult syncFromT3Excel(MultipartFile file, Integer warehouseId, Integer staffId) {

        // Build maps SKU -> catalog item
        Map<String, CatalogItem> skuToCatalog = partCatalogRepo.findAllParts().stream()
                .filter(p -> p.getSku() != null)
                .collect(Collectors.toMap(
                        p -> p.getSku().trim().toLowerCase(),
                        p -> p,
                        (a, b) -> a
                ));

        List<Brand> brands = catalogItemRepo.getAllBrands();
        Map<String, Brand> nameToBrand = brands.stream()
                .filter(b -> b.getBrandName() != null)
                .collect(Collectors.toMap(
                        b -> b.getBrandName().trim().toLowerCase(),
                        b -> b,
                        (a, b) -> a
                ));

        List<ItemCategory> categories = catalogItemRepo.getAllItemCategory();
        Map<String, ItemCategory> nameToCategory = categories.stream()
                .filter(c -> c.getCategoryName() != null)
                .collect(Collectors.toMap(
                        c -> c.getCategoryName().trim().toLowerCase(),
                        c -> c,
                        (a, b) -> a
                ));

        List<ProductLine> productLines = catalogItemRepo.getAllProductLines();
        List<com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa> allTaxRules = taxRuleRepositoryJpa.findAll();

        List<Row> rows = ExcelService.importFromExcel(file, row -> row);
        List<String> errors = new ArrayList<>();

        // Tạo stock_entry SYNC mới ở trạng thái DRAFT cho lần nhập này
        StockEntry syncEntry = buildSyncEntry(warehouseId, staffId);
        StockEntry savedEntry = stockEntryRepo.save(syncEntry);

        List<StockEntryItem> itemsToSave = new java.util.ArrayList<>();
        int inventoryUpdated = 0, inventoryInserted = 0;

        for (Row row : rows) {
            String sku = getCellString(row, COL_SKU);
            if (sku == null || sku.isBlank()) continue;
            String skuNorm = sku.trim().toLowerCase();

            int rowNum = row.getRowNum() + 1;
            String itemName = getCellString(row, COL_NAME);
            if (itemName == null || itemName.isBlank()) {
                errors.add("Dòng " + rowNum + ": Thiếu tên phụ tùng (SKU=" + sku + ")");
                continue;
            }

            String categoryName = getCellString(row, COL_CATEGORY);
            String brandName = getCellString(row, COL_BRAND);
            String productLineName = getCellString(row, COL_PRODUCT_LINE);
            String unit = getCellString(row, COL_UNIT);
            BigDecimal priceSell = getCellDecimal(row, COL_PRICE_SELL);
            if (priceSell == null) priceSell = BigDecimal.ZERO;

            String showPriceStr = getCellString(row, COL_SHOW_PRICE);
            Boolean showPrice = showPriceStr == null || !"Không".equalsIgnoreCase(showPriceStr.trim());

            Integer warranty = getCellInt(row, COL_WARRANTY);
            if (warranty == null) warranty = 0;

            String origin = getCellString(row, COL_ORIGIN);
            String color = getCellString(row, COL_COLOR);
            String compatibleCars = getCellString(row, COL_COMPATIBLE);
            String description = getCellString(row, COL_DESCRIPTION);
            String taxStr = getCellString(row, COL_TAX);

            CatalogItem catalogItem = skuToCatalog.get(skuNorm);
            
            // Resolve Brand, Category, Product Line
            Integer brandId = null;
            if (brandName != null && !brandName.isBlank()) {
                String brandNorm = brandName.trim().toLowerCase();
                Brand brand = nameToBrand.get(brandNorm);
                if (brand == null) {
                    brand = new Brand();
                    brand.setBrandName(brandName.trim());
                    brand.setIsActive((byte) 1);
                    brand = catalogItemRepo.createBrand(brand);
                    nameToBrand.put(brandNorm, brand);
                }
                brandId = brand.getBrandId();
            }

            Integer categoryId = null;
            if (categoryName != null && !categoryName.isBlank()) {
                String catNorm = categoryName.trim().toLowerCase();
                ItemCategory cat = nameToCategory.get(catNorm);
                if (cat == null) {
                    // File Excel ghi tên danh mục chứ không chọn từ danh sách, nên ở
                    // luồng này vẫn tạo danh mục mới khi chưa có.
                    cat = itemCategoryService.findOrCreateByName(categoryName);
                    nameToCategory.put(catNorm, cat);
                }
                categoryId = cat.getItemCategoryId();
            }

            Integer productLineId = null;
            if (productLineName != null && !productLineName.isBlank() && brandId != null) {
                String lineNorm = productLineName.trim().toLowerCase();
                final Integer bId = brandId;
                ProductLine line = productLines.stream()
                        .filter(l -> l.getBrandId() != null && l.getBrandId().equals(bId)
                                && l.getLineName() != null && l.getLineName().trim().toLowerCase().equals(lineNorm))
                        .findFirst()
                        .orElse(null);
                if (line == null) {
                    line = new ProductLine();
                    line.setBrandId(brandId);
                    line.setLineName(productLineName.trim());
                    line.setIsActive((byte) 1);
                    line = catalogItemRepo.saveProductLine(line);
                    productLines.add(line);
                }
                productLineId = line.getProductLineId();
            }

            Integer taxRuleId = resolveTaxRuleId(taxStr, allTaxRules);

            if (catalogItem == null) {
                // Tạo mới sản phẩm
                catalogItem = new CatalogItem();
                catalogItem.setSku(sku.trim());
                catalogItem.setItemName(itemName.trim());
                catalogItem.setItemType(com.g42.platform.gms.warehouse.domain.enums.CatalogItemType.PART);
                catalogItem.setUnit(unit);
                catalogItem.setPrice(priceSell);
                catalogItem.setShowPrice(showPrice);
                catalogItem.setWarrantyDurationMonths(warranty);
                catalogItem.setMadeIn(origin);
                catalogItem.setColor(color);
                catalogItem.setCompatibleCars(compatibleCars);
                catalogItem.setDescription(description);
                catalogItem.setTaxRuleId(taxRuleId);
                catalogItem.setIsActive(true);
                catalogItem.setBrandId(brandId);
                catalogItem.setItemCategoryId(categoryId);
                catalogItem.setProductLineId(productLineId);

                catalogItem = partCatalogRepo.save(catalogItem);
                skuToCatalog.put(skuNorm, catalogItem);
            } else {
                // Cập nhật thông tin sản phẩm nếu có thay đổi
                boolean needsSave = false;
                if (unit != null && !unit.isBlank() && !unit.equals(catalogItem.getUnit())) {
                    catalogItem.setUnit(unit);
                    needsSave = true;
                }
                if (priceSell.compareTo(BigDecimal.ZERO) > 0 && (catalogItem.getPrice() == null || priceSell.compareTo(catalogItem.getPrice()) != 0)) {
                    catalogItem.setPrice(priceSell);
                    needsSave = true;
                }
                if (showPrice != null && !showPrice.equals(catalogItem.getShowPrice())) {
                    catalogItem.setShowPrice(showPrice);
                    needsSave = true;
                }
                if (warranty > 0 && (catalogItem.getWarrantyDurationMonths() == null || !warranty.equals(catalogItem.getWarrantyDurationMonths()))) {
                    catalogItem.setWarrantyDurationMonths(warranty);
                    needsSave = true;
                }
                if (origin != null && !origin.isBlank() && !origin.equals(catalogItem.getMadeIn())) {
                    catalogItem.setMadeIn(origin);
                    needsSave = true;
                }
                if (color != null && !color.isBlank() && !color.equals(catalogItem.getColor())) {
                    catalogItem.setColor(color);
                    needsSave = true;
                }
                if (compatibleCars != null && !compatibleCars.isBlank() && !compatibleCars.equals(catalogItem.getCompatibleCars())) {
                    catalogItem.setCompatibleCars(compatibleCars);
                    needsSave = true;
                }
                if (description != null && !description.isBlank() && !description.equals(catalogItem.getDescription())) {
                    catalogItem.setDescription(description);
                    needsSave = true;
                }
                if (taxRuleId != null && !taxRuleId.equals(catalogItem.getTaxRuleId())) {
                    catalogItem.setTaxRuleId(taxRuleId);
                    needsSave = true;
                }
                if (brandId != null && !brandId.equals(catalogItem.getBrandId())) {
                    catalogItem.setBrandId(brandId);
                    needsSave = true;
                }
                if (categoryId != null && !categoryId.equals(catalogItem.getItemCategoryId())) {
                    catalogItem.setItemCategoryId(categoryId);
                    needsSave = true;
                }
                if (productLineId != null && !productLineId.equals(catalogItem.getProductLineId())) {
                    catalogItem.setProductLineId(productLineId);
                    needsSave = true;
                }

                if (needsSave) {
                    partCatalogRepo.save(catalogItem);
                }
            }

            Integer itemId = catalogItem.getItemId();
            BigDecimal qty = getCellQty(row, COL_QTY);

            if (qty != null && qty.signum() > 0) {
                BigDecimal importPrice = getCellDecimal(row, COL_PRICE_BUY);
                if (importPrice == null) importPrice = BigDecimal.ZERO;

                BigDecimal markup = getCellDecimal(row, COL_MARKUP);
                if (markup == null || markup.compareTo(BigDecimal.ZERO) <= 0) markup = new BigDecimal("1.3");

                String lotCode = getCellString(row, COL_LOT_CODE);
                String notes = lotCode != null && !lotCode.isBlank() ? "Lô " + lotCode : getCellString(row, COL_NOTE);
                if (notes == null || notes.isBlank()) {
                    notes = "Nhập thêm từ Excel";
                }

                StockEntryItem entryItem = StockEntryItem.builder()
                        .entryId(savedEntry.getEntryId())
                        .itemId(itemId)
                        .quantity(qty)
                        .importPrice(importPrice)
                        .markupMultiplier(markup)
                        .markupMultiplierWholesale(markup)
                        .remainingQuantity(qty)
                        .notes(notes)
                        .build();
                itemsToSave.add(entryItem);

                Inventory inv = inventoryRepo.findByWarehouseAndItem(warehouseId, itemId).orElse(null);
                if (inv != null) {
                    inventoryUpdated++;
                } else {
                    inventoryInserted++;
                }
            }
        }

        if (!itemsToSave.isEmpty()) {
            savedEntry.setItems(itemsToSave);
            stockEntryRepo.save(savedEntry);
        }

        return new SyncResult(savedEntry.getEntryId(), inventoryUpdated, inventoryInserted, errors);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private StockEntry buildSyncEntry(Integer warehouseId, Integer staffId) {
        return StockEntry.builder()
                .entryCode("SYNC-" + warehouseId + "-" + System.currentTimeMillis())
                .warehouseId(warehouseId)
                .supplierName("SYNC - Nhập thêm từ Excel")
                .entryDate(LocalDate.now())
                .status(StockEntryStatus.DRAFT)
                .notes("Nhập thêm tồn kho từ file Excel (Bản nháp)")
                .createdBy(staffId)
                .build();
    }

    private String generateCategoryCode(String name) {
        String clean = name != null ? name : "";
        String code = clean.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (code.length() > 10) {
            code = code.substring(0, 10);
        }
        if (code.isEmpty()) {
            code = "CATE";
        }
        return code + "-" + (System.currentTimeMillis() % 1000);
    }

    private Integer resolveTaxRuleId(String taxStr, List<com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa> allTaxRules) {
        if (taxStr == null || taxStr.isBlank()) {
            return null;
        }
        String clean = taxStr.trim().toLowerCase();
        if (clean.contains("free") || clean.contains("miễn") || clean.contains("0")) {
            return allTaxRules.stream()
                    .filter(t -> "FREE".equalsIgnoreCase(t.getTaxCode()))
                    .map(com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa::getTaxRuleId)
                    .findFirst()
                    .orElse(null);
        }

        double percentValue = -1;
        try {
            String numStr = clean.replaceAll("[^0-9.]", "");
            if (!numStr.isEmpty()) {
                double val = Double.parseDouble(numStr);
                if (val > 1) {
                    percentValue = val / 100.0;
                } else {
                    percentValue = val;
                }
            }
        } catch (Exception ignored) {}

        if (percentValue >= 0) {
            final double targetRate = percentValue;
            Optional<com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa> match = allTaxRules.stream()
                    .filter(t -> t.getTaxRate() != null && Math.abs(t.getTaxRate().doubleValue() - targetRate) < 0.001)
                    .findFirst();
            if (match.isPresent()) {
                return match.get().getTaxRuleId();
            }
        }

        return allTaxRules.stream()
                .filter(t -> (t.getTaxName() != null && t.getTaxName().toLowerCase().contains(clean))
                        || (t.getTaxCode() != null && t.getTaxCode().toLowerCase().contains(clean)))
                .map(com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa::getTaxRuleId)
                .findFirst()
                .orElse(null);
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return null;
    }

    /** Số lượng có thể lẻ (lít, kg): đọc nguyên giá trị thập phân, không làm tròn. */
    private BigDecimal getCellQty(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
                return Qty.normalize(BigDecimal.valueOf(cell.getNumericCellValue()));
            }
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim().replace(",", ".");
                return s.isEmpty() ? null : Qty.normalize(new BigDecimal(s));
            }
        } catch (Exception e) {
            return null;
        }
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
