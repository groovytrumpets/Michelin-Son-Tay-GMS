package com.g42.platform.gms.warehouse.app.service.entry;

import com.g42.platform.gms.common.service.ExcelService;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.StockEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryResponse;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service xử lý import/export Excel cho phiếu nhập kho.
 *
 * Format Excel nhập kho (sheet 1):
 * | STT | SKU | Tên phụ tùng | Số lượng | Giá nhập (VNĐ) | Hệ số markup | Ghi chú |
 *
 * - SKU: bắt buộc — dùng để tìm itemId trong catalog
 * - Nếu SKU không tồn tại → báo lỗi dòng đó, bỏ qua
 * - Số lượng: số nguyên dương
 * - Giá nhập: số thực dương
 * - Hệ số markup: tùy chọn, mặc định 1.3 nếu để trống
 */
@Service
@RequiredArgsConstructor
public class StockEntryExcelService {

    private final PartCatalogRepo partCatalogRepo;
    private final StockEntryService stockEntryService;

    private static final String[] IMPORT_HEADERS = {
            "STT", "SKU", "Tên phụ tùng", "Số lượng", "Giá nhập (VNĐ)", "Hệ số markup", "Ghi chú"
    };

    /**
     * Tải về file Excel mẫu để nhập kho.
     * Có sẵn 2 dòng ví dụ.
     */
    public byte[] downloadTemplate() {
        List<String[]> sampleData = List.of(
                new String[]{"1", "FILTER-BOSCH-001", "Lọc dầu Bosch", "20", "45000", "1.5", "Lô tháng 4"},
                new String[]{"2", "TIRE-MICH-205", "Lốp Michelin 205/55R16", "10", "1200000", "1.3", ""}
        );

        return ExcelService.exportToExcel(sampleData, IMPORT_HEADERS, row -> row);
    }

    /**
     * Tải về danh sách tất cả PART trong catalog dưới dạng Excel
     * để thủ kho tham khảo SKU khi điền phiếu nhập.
     */
    public byte[] exportCatalogTemplate() {
        Specification<CatalogItemJpa> spec = (root, query, cb) ->
                cb.equal(root.get("itemType"), CatalogItemType.PART);
        List<CatalogItemJpa> parts = partCatalogRepo.findAll(spec);

        String[] headers = {"STT", "SKU", "Tên phụ tùng", "Đơn vị", "Part Number", "Barcode"};
        int[] stt = {1};
        return ExcelService.exportToExcel(parts, headers, part -> new Object[]{
                stt[0]++,
                part.getSku() != null ? part.getSku() : "",
                part.getItemName(),
                part.getUnit() != null ? part.getUnit() : "",
                part.getPartNumber() != null ? part.getPartNumber() : "",
                part.getBarcode() != null ? part.getBarcode() : ""
        });
    }

    /**
     * Import phiếu nhập kho từ file Excel.
     * Mỗi dòng = 1 item trong phiếu nhập.
     * Tất cả dòng hợp lệ sẽ được gộp vào 1 phiếu nhập duy nhất.
     *
     * @param file       File Excel upload
     * @param warehouseId Kho nhập
     * @param supplierName Tên nhà cung cấp
     * @param staffId    ID nhân viên thực hiện
     * @return StockEntryResponse phiếu nhập đã tạo
     */
    @Transactional
    public StockEntryImportResult importStockEntry(
            MultipartFile file,
            Integer warehouseId,
            String supplierName,
            Integer staffId) {

        // Build SKU → itemId map từ catalog
        Specification<CatalogItemJpa> spec = (root, query, cb) ->
                cb.equal(root.get("itemType"), CatalogItemType.PART);
        Map<String, Integer> skuToItemId = partCatalogRepo.findAll(spec).stream()
                .filter(p -> p.getSku() != null)
                .collect(Collectors.toMap(
                        p -> p.getSku().trim().toLowerCase(),
                        CatalogItemJpa::getItemId,
                        (a, b) -> a // giữ cái đầu nếu trùng SKU
                ));

        List<StockEntryItemRequest> validItems = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        List<Row> rows = ExcelService.importFromExcel(file, row -> row);

        for (Row row : rows) {
            int rowNum = row.getRowNum() + 1; // 1-indexed cho user

            String sku = getCellString(row, 1); // cột B
            if (sku == null || sku.isBlank()) {
                errors.add("Dòng " + rowNum + ": SKU không được để trống");
                continue;
            }

            Integer itemId = skuToItemId.get(sku.trim().toLowerCase());
            if (itemId == null) {
                errors.add("Dòng " + rowNum + ": SKU '" + sku + "' không tồn tại trong catalog");
                continue;
            }

            Integer quantity = getCellInt(row, 3); // cột D
            if (quantity == null || quantity <= 0) {
                errors.add("Dòng " + rowNum + ": Số lượng phải là số nguyên dương (đọc được: " + getCellRaw(row, 3) + ")");
                continue;
            }

            BigDecimal importPrice = getCellDecimal(row, 4); // cột E
            if (importPrice == null || importPrice.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Dòng " + rowNum + ": Giá nhập phải lớn hơn 0");
                continue;
            }

            BigDecimal markup = getCellDecimal(row, 5); // cột F
            if (markup == null || markup.compareTo(BigDecimal.ZERO) <= 0) {
                markup = new BigDecimal("1.3"); // mặc định
            }

            String notes = getCellString(row, 6); // cột G

            StockEntryItemRequest item = new StockEntryItemRequest();
            item.setItemId(itemId);
            item.setQuantity(quantity);
            item.setImportPrice(importPrice);
            item.setMarkupMultiplier(markup);
            item.setNotes(notes);
            validItems.add(item);
        }

        if (validItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không có dòng hợp lệ nào. Lỗi: " + String.join("; ", errors));
        }

        // Tạo phiếu nhập
        CreateStockEntryRequest request = new CreateStockEntryRequest();
        request.setWarehouseId(warehouseId);
        request.setSupplierName(supplierName);
        request.setEntryDate(LocalDate.now());
        request.setNotes("Import từ Excel");
        request.setItems(validItems);

        StockEntryResponse entry = stockEntryService.create(request, staffId);

        return new StockEntryImportResult(entry, errors, validItems.size());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String getCellRaw(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "null";
        return "type=" + cell.getCellType() + ",val=" + (cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : cell.toString());
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
        if (cell.getCellType() == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            return (int) Math.round(val);
        }
        if (cell.getCellType() == CellType.STRING) {
            String s = cell.getStringCellValue().trim();
            if (s.isEmpty()) return null;
            try {
                // Xử lý cả "20" và "20.0"
                return (int) Math.round(Double.parseDouble(s));
            } catch (Exception e) { return null; }
        }
        if (cell.getCellType() == CellType.FORMULA) {
            try { return (int) Math.round(cell.getNumericCellValue()); } catch (Exception e) { return null; }
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
        if (cell.getCellType() == CellType.FORMULA) {
            try { return BigDecimal.valueOf(cell.getNumericCellValue()); } catch (Exception e) { return null; }
        }
        return null;
    }

    // ── Inner result class ────────────────────────────────────────────────────

    public record StockEntryImportResult(
            StockEntryResponse entry,
            List<String> errors,
            int importedCount
    ) {}
}
