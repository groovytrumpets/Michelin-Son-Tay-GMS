package com.g42.platform.gms.warehouse.api.controller.returns;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryFormRequest;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchReturnItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateReturnEntryRequest;
import com.g42.platform.gms.warehouse.api.dto.response.ReturnEntryResponse;
import com.g42.platform.gms.warehouse.app.service.returns.ReturnEntryService;
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
@RequestMapping("/api/warehouse/return-entries")
@RequiredArgsConstructor
public class ReturnEntryController {

    private final ReturnEntryService returnEntryService;

    /** Danh sách phiếu hoàn theo kho, mới nhất trước */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReturnEntryResponse>>> list(
            @RequestParam Integer warehouseId) {
        return ResponseEntity.ok(ApiResponses.success(
                returnEntryService.listByWarehouse(warehouseId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReturnEntryResponse>> create(
            @Valid @RequestBody CreateReturnEntryRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                returnEntryService.create(request, principal.getStaffId())));
    }

    /**
     * Tạo phiếu hoàn + ảnh lỗi từng item trong 1 form (multipart/form-data).
     * items: JSON string. file_0..file_4: ảnh lỗi theo index item.
     */
    @PostMapping(value = "/with-attachments", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReturnEntryResponse>> createWithAttachments(
            @Valid @ModelAttribute CreateReturnEntryFormRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponses.success(
                returnEntryService.createWithAttachments(request, principal.getStaffId())));
    }

    /**
     * Upload ảnh lỗi cho từng sản phẩm trong phiếu hoàn.
     * Path: /api/warehouse/return-entries/items/{returnItemId}/attachments
     */
    @PostMapping("/items/{returnItemId}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> addAttachment(
            @PathVariable Integer returnItemId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal StaffPrincipal principal) throws IOException {
        returnEntryService.addAttachment(returnItemId, file, principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(null));
    }

    @PatchMapping("/{id}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReturnEntryResponse>> patchItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId,
            @RequestBody PatchReturnItemRequest request) {
        return ResponseEntity.ok(ApiResponses.success(returnEntryService.patchItem(id, itemId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReturnEntryResponse>> update(
            @PathVariable Integer id,
            @RequestBody UpdateReturnEntryRequest request) {
        return ResponseEntity.ok(ApiResponses.success(returnEntryService.update(id, request)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReturnEntryResponse>> confirm(
            @PathVariable Integer id,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                returnEntryService.confirm(id, principal.getStaffId())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReturnEntryResponse>> getDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponses.success(returnEntryService.getDetail(id)));
    }
}
