package com.g42.platform.gms.warehouse.api.controller.returns;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.request.CreateReturnEntryRequest;
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

@RestController
@RequestMapping("/api/warehouse/return-entries")
@RequiredArgsConstructor
public class ReturnEntryController {

    private final ReturnEntryService returnEntryService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReturnEntryResponse>> create(
            @Valid @RequestBody CreateReturnEntryRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                returnEntryService.create(request, principal.getStaffId())));
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

    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReturnEntryResponse>> confirm(
            @PathVariable Integer id,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                returnEntryService.confirm(id, principal.getStaffId())));
    }
}
