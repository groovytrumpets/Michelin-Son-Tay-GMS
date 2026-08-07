package com.g42.platform.gms.vehicle.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.vehicle.dto.VehicleBrandDto;
import com.g42.platform.gms.vehicle.service.VehicleBrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Phần đọc của danh mục hãng xe / dòng xe.
 * Nằm dưới /api/vehicles/** (permitAll) vì trang tra cứu phụ tùng cho khách
 * cũng cần danh sách này. Thao tác sửa danh mục nằm ở
 * {@link VehicleBrandController} dưới /api/admin/vehicle-brands.
 */
@RestController
@RequestMapping("/api/vehicles/brands")
@RequiredArgsConstructor
public class VehicleBrandCatalogController {

    private final VehicleBrandService vehicleBrandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleBrandDto>>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "true") boolean withModels) {
        return ResponseEntity.ok(ApiResponses.success(vehicleBrandService.findAll(activeOnly, withModels)));
    }

    @GetMapping("/{brandId}/models")
    public ResponseEntity<ApiResponse<List<String>>> models(
            @PathVariable Integer brandId,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(ApiResponses.success(vehicleBrandService.findModels(brandId, activeOnly)));
    }
}
