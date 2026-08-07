package com.g42.platform.gms.vehicle.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import com.g42.platform.gms.vehicle.dto.VehicleBrandDto;
import com.g42.platform.gms.vehicle.service.VehicleBrandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Cấu hình danh mục hãng xe / dòng xe.
 * Đặt dưới /api/admin/** để yêu cầu đăng nhập — /api/vehicles/** đang permitAll
 * nên chỉ dùng cho phần đọc (xem {@link VehicleBrandCatalogController}).
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/vehicle-brands")
@RequiredArgsConstructor
public class VehicleBrandController {

    private final VehicleBrandService vehicleBrandService;

    @PostMapping
    @Auditable(action = "CREATE", module = "VEHICLE", description = "Thêm hãng xe", targetType = "VEHICLE_BRAND")
    public ResponseEntity<ApiResponse<VehicleBrandDto>> create(@RequestBody VehicleBrandDto dto) {
        try {
            return ResponseEntity.ok(ApiResponses.success(vehicleBrandService.createBrand(dto)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponses.error("INVALID_BRAND", e.getMessage()));
        }
    }

    @PutMapping("/{brandId}")
    @Auditable(action = "UPDATE", module = "VEHICLE", description = "Cập nhật hãng xe", targetType = "VEHICLE_BRAND")
    public ResponseEntity<ApiResponse<VehicleBrandDto>> update(@PathVariable Integer brandId,
                                                               @RequestBody VehicleBrandDto dto) {
        try {
            return ResponseEntity.ok(ApiResponses.success(vehicleBrandService.updateBrand(brandId, dto)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponses.error("INVALID_BRAND", e.getMessage()));
        }
    }

    @DeleteMapping("/{brandId}")
    @Auditable(action = "DELETE", module = "VEHICLE", severity = "WARNING", description = "Ngừng sử dụng hãng xe", targetType = "VEHICLE_BRAND")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Integer brandId) {
        try {
            vehicleBrandService.deactivateBrand(brandId);
            return ResponseEntity.ok(ApiResponses.successMessage("Đã ngừng sử dụng hãng xe"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponses.error("INVALID_BRAND", e.getMessage()));
        }
    }

    @PostMapping("/{brandId}/models")
    @Auditable(action = "CREATE", module = "VEHICLE", description = "Thêm dòng xe", targetType = "VEHICLE_MODEL")
    public ResponseEntity<ApiResponse<String>> addModel(@PathVariable Integer brandId,
                                                        @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(ApiResponses.success(vehicleBrandService.addModel(brandId, body.get("name"))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponses.error("INVALID_MODEL", e.getMessage()));
        }
    }

    /** Nạp bổ sung dòng xe từ API công khai NHTSA vPIC (miễn phí, không cần key). */
    @PostMapping("/{brandId}/models/import")
    @Auditable(action = "CREATE", module = "VEHICLE", description = "Nhập dòng xe từ NHTSA", targetType = "VEHICLE_MODEL")
    public ResponseEntity<ApiResponse<Integer>> importModels(@PathVariable Integer brandId) {
        try {
            int imported = vehicleBrandService.importModelsFromNhtsa(brandId);
            return ResponseEntity.ok(ApiResponses.success(imported,
                    imported > 0 ? "Đã bổ sung " + imported + " dòng xe" : "Không có dòng xe mới"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponses.error("INVALID_BRAND", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiResponses.error("LOOKUP_FAILED", e.getMessage()));
        }
    }
}
