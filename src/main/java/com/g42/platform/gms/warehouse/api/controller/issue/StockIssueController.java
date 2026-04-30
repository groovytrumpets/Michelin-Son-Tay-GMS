package com.g42.platform.gms.warehouse.api.controller.issue;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueWithAttachmentRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchIssueItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueDetailResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;
import com.g42.platform.gms.warehouse.app.service.issue.StockIssueService;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse/stock-issues")
@RequiredArgsConstructor
public class StockIssueController {

    private final StockIssueService stockIssueService;

    /** Danh sách phiếu xuất theo kho, mới nhất trước */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockIssueResponse>>> list(
            @RequestParam Integer warehouseId,
            @RequestParam(required = false) StockIssueStatus status,
            @RequestParam(required = false) IssueType issueType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponses.success(
                stockIssueService.searchByWarehouse(warehouseId, status, issueType, fromDate, toDate, search, page, size)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockIssueResponse>> create(
            @Valid @RequestBody CreateStockIssueRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                stockIssueService.create(request, principal.getStaffId())));
    }

    /**
     * Tạo phiếu xuất kho + ảnh chứng từ trong 1 form (multipart/form-data).
     * Dùng @ModelAttribute — tương tự CheckIn complete-all.
     * items truyền dưới dạng JSON string.
     * POST /api/warehouse/stock-issues/with-attachment
     */
    @PostMapping(value = "/with-attachment", consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockIssueResponse>> createWithAttachment(
            @Valid @ModelAttribute CreateStockIssueWithAttachmentRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) throws IOException {
        return ResponseEntity.ok(ApiResponses.success(
                stockIssueService.createWithAttachmentForm(request, principal.getStaffId())));
    }

    /** Sửa từng item trong phiếu xuất — chỉ khi DRAFT */
    @PatchMapping("/{id}/items/{itemId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockIssueResponse>> patchItem(
            @PathVariable Integer id,
            @PathVariable Integer itemId,
            @RequestBody PatchIssueItemRequest request) {
        return ResponseEntity.ok(ApiResponses.success(stockIssueService.patchItem(id, itemId, request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockIssueResponse>> update(
            @PathVariable Integer id,
            @RequestBody UpdateStockIssueRequest request) {
        return ResponseEntity.ok(ApiResponses.success(stockIssueService.update(id, request)));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockIssueResponse>> confirm(
            @PathVariable Integer id,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                stockIssueService.confirm(id, principal.getStaffId())));
    }

        /** Hủy phiếu ở trạng thái DRAFT; nếu là SERVICE_TICKET thì trả reservation về kho */
        @PostMapping("/{id}/cancel")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<ApiResponse<StockIssueResponse>> cancel(
            @PathVariable Integer id,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
            stockIssueService.cancel(id, principal.getStaffId())));
        }

    /** Upload ảnh riêng cho phiếu đã tạo */
    @PostMapping("/{id}/attachments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> addAttachment(
            @PathVariable Integer id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal StaffPrincipal principal) throws IOException {
        stockIssueService.addAttachment(id, file, principal.getStaffId());
        return ResponseEntity.ok(ApiResponses.success(null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockIssueDetailResponse>> getDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponses.success(stockIssueService.getDetail(id)));
    }
}
