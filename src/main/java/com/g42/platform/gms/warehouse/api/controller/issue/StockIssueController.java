package com.g42.platform.gms.warehouse.api.controller.issue;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchIssueItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueDetailResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;
import com.g42.platform.gms.warehouse.app.service.issue.StockIssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/stock-issues")
@RequiredArgsConstructor
public class StockIssueController {

    private final StockIssueService stockIssueService;

    /** Danh sách phiếu xuất theo kho, mới nhất trước */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockIssueResponse>>> list(
            @RequestParam Integer warehouseId) {
        return ResponseEntity.ok(ApiResponses.success(
                stockIssueService.listByWarehouse(warehouseId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockIssueResponse>> create(
            @Valid @RequestBody CreateStockIssueRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(
                stockIssueService.create(request, principal.getStaffId())));
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

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockIssueDetailResponse>> getDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponses.success(stockIssueService.getDetail(id)));
    }
}
