package com.g42.platform.gms.warehouse.api.controller.catalog;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.request.CreatePartRequest;
import com.g42.platform.gms.warehouse.api.dto.response.PartResponse;
import com.g42.platform.gms.warehouse.app.service.catalog.WarehouseCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/catalog")
@RequiredArgsConstructor
public class WarehouseCatalogController {

    private final WarehouseCatalogService warehouseCatalogService;

    /**
     * Tìm kiếm part/service theo tên, SKU, part number, barcode.
     * Dùng khi thêm item vào phiếu nhập kho.
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PartResponse>>> search(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponses.success(
                warehouseCatalogService.search(keyword)));
    }

    /**
     * Tạo mới một PART (phụ tùng) — dùng khi nhập kho mà chưa có trong catalog.
     * Chỉ MANAGER / ADMIN / WAREHOUSE_KEEPER mới được tạo.
     */
    @PostMapping("/parts")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','WAREHOUSE_KEEPER')")
    public ResponseEntity<ApiResponse<PartResponse>> createPart(
            @Valid @RequestBody CreatePartRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        PartResponse created = warehouseCatalogService.createPart(request, principal.getStaffId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(created));
    }
}
