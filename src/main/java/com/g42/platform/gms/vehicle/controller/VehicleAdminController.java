package com.g42.platform.gms.vehicle.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import com.g42.platform.gms.vehicle.dto.VehicleUpdateRequest;
import com.g42.platform.gms.vehicle.dto.VehicleUpdateResponse;
import com.g42.platform.gms.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Staff-only vehicle management endpoints.
 * Nằm ngoài /api/vehicles/** (permitAll) vì đây là thao tác ghi dữ liệu,
 * yêu cầu đăng nhập nhân viên (chặn bởi anyRequest().authenticated()).
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/vehicles")
@RequiredArgsConstructor
public class VehicleAdminController {

    private final VehicleService vehicleService;

    /**
     * Sửa biển số/hãng/dòng/năm sản xuất của một xe đã có trong hệ thống.
     * Dùng ở popup "Chỉnh sửa hồ sơ khách hàng" -> tab "Xe của khách hàng".
     * PUT /api/admin/vehicles/{vehicleId}
     */
    @PutMapping("/{vehicleId}")
    @Auditable(action = "UPDATE", module = "CUSTOMER", description = "Cập nhật thông tin xe của khách hàng", targetType = "VEHICLE")
    public ResponseEntity<ApiResponse<VehicleUpdateResponse>> updateVehicle(
            @PathVariable Integer vehicleId,
            @Valid @RequestBody VehicleUpdateRequest request) {
        try {
            VehicleUpdateResponse updated = vehicleService.updateVehicle(vehicleId, request);
            return ResponseEntity.ok(ApiResponses.success(updated));
        } catch (RuntimeException e) {
            log.error("Update vehicle failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error("VEHICLE_UPDATE_FAILED", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error updating vehicle", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponses.error("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }
}
