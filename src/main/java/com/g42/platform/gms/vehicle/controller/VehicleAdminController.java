package com.g42.platform.gms.vehicle.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.systemlog.annotation.Auditable;
import com.g42.platform.gms.vehicle.dto.PlateOwnerDto;
import com.g42.platform.gms.vehicle.dto.VehicleCreateRequest;
import com.g42.platform.gms.vehicle.dto.VehicleUpdateRequest;
import com.g42.platform.gms.vehicle.dto.VehicleUpdateResponse;
import com.g42.platform.gms.vehicle.service.SharedPlateException;
import com.g42.platform.gms.vehicle.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Thêm xe cho khách hàng từ màn /vehicle-management.
     * POST /api/admin/vehicles
     */
    /**
     * Các hồ sơ khách đang gắn biển số này. Một biển số dùng chung được cho nhiều khách
     * (vợ chồng, gia đình, công ty) nên trả về danh sách để màn hình cho chọn đúng người.
     * GET /api/admin/vehicles/by-plate?licensePlate=30K-86694
     */
    @GetMapping("/by-plate")
    public ResponseEntity<ApiResponse<List<PlateOwnerDto>>> findByPlate(@RequestParam String licensePlate) {
        return ResponseEntity.ok(ApiResponses.success(vehicleService.findOwnersByPlate(licensePlate)));
    }

    @PostMapping
    @Auditable(action = "CREATE", module = "CUSTOMER", description = "Thêm xe cho khách hàng", targetType = "VEHICLE")
    public ResponseEntity<ApiResponse<?>> createVehicle(
            @Valid @RequestBody VehicleCreateRequest request) {
        try {
            return ResponseEntity.ok(ApiResponses.success(vehicleService.createVehicle(request)));
        } catch (SharedPlateException e) {
            // Chưa phải lỗi: hỏi lại nhân viên rồi gửi kèm allowSharedPlate = true là ghi được
            log.info("Create vehicle needs shared-plate confirmation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponses.error(SharedPlateException.CODE, e.getMessage(), e.getOwners()));
        } catch (RuntimeException e) {
            log.error("Create vehicle failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error("VEHICLE_CREATE_FAILED", e.getMessage()));
        }
    }

    /**
     * Xoá xe chưa phát sinh lịch sử (phiếu dịch vụ, lịch hẹn, sổ cũ...).
     * DELETE /api/admin/vehicles/{vehicleId}
     */
    @DeleteMapping("/{vehicleId}")
    @Auditable(action = "DELETE", module = "CUSTOMER", severity = "WARNING", description = "Xoá xe của khách hàng", targetType = "VEHICLE")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Integer vehicleId) {
        try {
            vehicleService.deleteVehicle(vehicleId);
            return ResponseEntity.ok(ApiResponses.success(null));
        } catch (DataIntegrityViolationException e) {
            log.error("Delete vehicle {} blocked by FK: {}", vehicleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error("VEHICLE_IN_USE", "Xe đã phát sinh dữ liệu liên quan nên không thể xoá."));
        } catch (RuntimeException e) {
            log.error("Delete vehicle failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error("VEHICLE_DELETE_FAILED", e.getMessage()));
        }
    }

    /**
     * Sửa biển số/hãng/dòng/năm sản xuất của một xe đã có trong hệ thống.
     * Dùng ở popup "Chỉnh sửa hồ sơ khách hàng" -> tab "Xe của khách hàng".
     * PUT /api/admin/vehicles/{vehicleId}
     */
    @PutMapping("/{vehicleId}")
    @Auditable(action = "UPDATE", module = "CUSTOMER", description = "Cập nhật thông tin xe của khách hàng", targetType = "VEHICLE")
    public ResponseEntity<ApiResponse<?>> updateVehicle(
            @PathVariable Integer vehicleId,
            @Valid @RequestBody VehicleUpdateRequest request) {
        try {
            VehicleUpdateResponse updated = vehicleService.updateVehicle(vehicleId, request);
            return ResponseEntity.ok(ApiResponses.success(updated));
        } catch (SharedPlateException e) {
            log.info("Update vehicle needs shared-plate confirmation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponses.error(SharedPlateException.CODE, e.getMessage(), e.getOwners()));
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
