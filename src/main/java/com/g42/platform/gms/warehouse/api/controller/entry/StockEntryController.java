package com.g42.platform.gms.warehouse.api.controller.entry;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryWithAttachmentRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryResponse;
import com.g42.platform.gms.warehouse.app.service.entry.StockEntryService;
import com.g42.platform.gms.warehouse.api.dto.request.PatchEntryItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryImportResponse;
import com.g42.platform.gms.warehouse.app.service.entry.StockEntryExcelService;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse/stock-entries")
@RequiredArgsConstructor
public class StockEntryController {

    private final StockEntryService stockEntryService;
    private final StockEntryExcelService excelService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<Page<StockEntryResponse>>> list(
            @RequestParam Integer warehouseId,
                        @RequestParam(required = false) StockEntryStatus status,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                        @RequestParam(required = false) String search,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponses.success(
                                stockEntryService.searchByWarehouse(warehouseId, status, fromDate, toDate, search, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockEntryResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponses.success(stockEntryService.getById(id)));
    }

    /** Sửa từng item trong phiếu nhập — chỉ khi DRAFT */
    @PatchMapping("/{id}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockEntryResponse>> patchItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId,
            @RequestBody PatchEntryItemRequest request) {
        return ResponseEntity.ok(ApiResponses.success(stockEntryService.patchItem(id, itemId, request)));
    }

    /** Cập nhật phiếu nhập kho — chỉ khi DRAFT */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockEntryResponse>> update(
            @PathVariable Integer id,
            @RequestBody UpdateStockEntryRequest request) {
        return ResponseEntity.ok(ApiResponses.success(stockEntryService.update(id, request)));
    }

    /**
     * Tạo phiếu nhập kho + ảnh chứng từ trong 1 form (multipart/form-data).
     * Dùng @ModelAttribute — tương tự CheckIn complete-all.
     * items truyền dưới dạng JSON string.
     * POST /api/warehouse/stock-entries/with-attachment
     */
    @PostMapping(value = "/with-attachment", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockEntryResponse>> createWithAttachment(
            @Valid @ModelAttribute CreateStockEntryWithAttachmentRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponses.success(
                stockEntryService.createWithAttachmentForm(request, principal.getStaffId())));
    }

    /** Tạo phiếu DRAFT (JSON only, không kèm ảnh) */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockEntryResponse>> create(
            @Valid @RequestBody CreateStockEntryRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                stockEntryService.create(request, principal.getStaffId())));
    }

    /** Upload ảnh riêng cho phiếu đã tạo */
    @PostMapping("/{id}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> addAttachment(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal StaffPrincipal principal) throws IOException {
        stockEntryService.addAttachment(id, file, principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(null));
    }

    /** Xác nhận phiếu nhập → cộng inventory */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockEntryResponse>> confirm(
            @PathVariable Integer id,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                stockEntryService.confirm(id, principal.getStaffId())));
    }

    // ── Excel ──────────────────────────────────────────────────────────────

    /**
     * Xuất danh sách phiếu nhập kho ra Excel.
     * GET /api/warehouse/stock-entries/excel/export?warehouseId=1
     */
    @GetMapping("/excel/export")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','MANAGER','ADMIN','ACCOUNTANT')")
    public ResponseEntity<byte[]> exportStockEntries(
            @RequestParam Integer warehouseId,
            @RequestParam(required = false) StockEntryStatus status) {

        List<StockEntryResponse> entries = stockEntryService.listByWarehouse(warehouseId, status);

        String[] headers = {"STT", "Mã phiếu", "Nhà cung cấp", "Ngày nhập",
                "Trạng thái", "Số loại hàng", "Ghi chú", "Ngày tạo"};
        int[] stt = {1};
        byte[] bytes = com.g42.platform.gms.common.service.ExcelService.exportToExcel(entries, headers, e -> new Object[]{
                stt[0]++,
                e.getEntryCode(),
                e.getSupplierName(),
                e.getEntryDate() != null ? e.getEntryDate().toString() : "",
                e.getStatus() != null ? e.getStatus().name() : "",
                e.getItems() != null ? e.getItems().size() : 0,
                e.getNotes() != null ? e.getNotes() : "",
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : ""
        });

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"stock-entries.xlsx\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * Tải về file Excel mẫu để nhập kho.
     * GET /api/warehouse/stock-entries/excel/template
     */
    @GetMapping("/excel/template")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = excelService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stock-entry-template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * Tải về danh sách tất cả PART trong catalog (để tham khảo SKU khi điền phiếu nhập).
     * GET /api/warehouse/stock-entries/excel/catalog
     */
    @GetMapping("/excel/catalog")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadCatalog() {
        byte[] bytes = excelService.exportCatalogTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"catalog-parts.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /**
     * Import phiếu nhập kho từ file Excel.
     * POST /api/warehouse/stock-entries/excel/import
     * Content-Type: multipart/form-data
     * Params: file, warehouseId, supplierName
     */
    @PostMapping(value = "/excel/import", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('WAREHOUSE_KEEPER','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<StockEntryImportResponse>> importFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam Integer warehouseId,
            @RequestParam String supplierName,
            @AuthenticationPrincipal StaffPrincipal principal) {

        StockEntryExcelService.StockEntryImportResult result =
                excelService.importStockEntry(file, warehouseId, supplierName, principal.getStaffId());

        StockEntryImportResponse response = new StockEntryImportResponse();
        response.setEntry(result.entry());
        response.setImportedCount(result.importedCount());
        response.setErrors(result.errors());
        response.setHasErrors(!result.errors().isEmpty());

        return ResponseEntity.ok(ApiResponses.success(response));
    }
}
