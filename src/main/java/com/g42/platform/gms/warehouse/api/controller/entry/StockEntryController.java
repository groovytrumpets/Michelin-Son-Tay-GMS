package com.g42.platform.gms.warehouse.api.controller.entry;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.entry.CreateStockEntryWithAttachmentRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockEntryResponse;
import com.g42.platform.gms.warehouse.app.service.entry.StockEntryService;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse/stock-entries")
@RequiredArgsConstructor
public class StockEntryController {

    private final StockEntryService stockEntryService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockEntryResponse>>> list(
            @RequestParam Integer warehouseId,
            @RequestParam(required = false) StockEntryStatus status) {
        return ResponseEntity.ok(ApiResponses.success(
                stockEntryService.listByWarehouse(warehouseId, status)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockEntryResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponses.success(stockEntryService.getById(id)));
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
}
