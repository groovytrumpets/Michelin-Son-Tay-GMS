package com.g42.platform.gms.manager.attendancelocation.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.manager.attendancelocation.api.dto.AttendanceLocationRequest;
import com.g42.platform.gms.manager.attendancelocation.api.dto.AttendanceLocationResponse;
import com.g42.platform.gms.manager.attendancelocation.application.service.AttendanceLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/attendance-locations")
@RequiredArgsConstructor
public class AttendanceLocationController {

    private final AttendanceLocationService service;

    /**
     * Lấy danh sách vị trí chấm công
     */
    @GetMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.ATTENDANCE_LOCATION_VIEW + "')")
    public ResponseEntity<ApiResponse<List<AttendanceLocationResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponses.success(service.getAllLocations()));
    }

    /**
     * Tạo vị trí chấm công mới (tự sinh mã QR)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.ATTENDANCE_LOCATION_EDIT + "')")
    public ResponseEntity<ApiResponse<AttendanceLocationResponse>> create(
            @Valid @RequestBody AttendanceLocationRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.createLocation(request), "Tạo vị trí chấm công thành công"));
    }

    /**
     * Cập nhật vị trí chấm công (không đổi mã QR)
     */
    @PutMapping("/{locationId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ATTENDANCE_LOCATION_EDIT + "')")
    public ResponseEntity<ApiResponse<AttendanceLocationResponse>> update(
            @PathVariable Integer locationId,
            @Valid @RequestBody AttendanceLocationRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.updateLocation(locationId, request), "Cập nhật vị trí chấm công thành công"));
    }

    /**
     * Vô hiệu hóa vị trí (nhân viên không thể chấm công tại đây nữa)
     */
    @PutMapping("/{locationId}/deactivate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ATTENDANCE_LOCATION_EDIT + "')")
    public ResponseEntity<ApiResponse<String>> deactivate(@PathVariable Integer locationId) {
        service.deactivateLocation(locationId);
        return ResponseEntity.ok(ApiResponses.success("Đã vô hiệu hóa vị trí chấm công"));
    }

    /**
     * Khôi phục vị trí đã vô hiệu hóa
     */
    @PutMapping("/{locationId}/reactivate")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ATTENDANCE_LOCATION_EDIT + "')")
    public ResponseEntity<ApiResponse<String>> reactivate(@PathVariable Integer locationId) {
        service.reactivateLocation(locationId);
        return ResponseEntity.ok(ApiResponses.success("Đã khôi phục vị trí chấm công"));
    }

    /**
     * Tạo lại mã QR cho vị trí (thu hồi mã QR cũ, cần in lại)
     */
    @PostMapping("/{locationId}/regenerate-qr")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ATTENDANCE_LOCATION_EDIT + "')")
    public ResponseEntity<ApiResponse<AttendanceLocationResponse>> regenerateQr(@PathVariable Integer locationId) {
        return ResponseEntity.ok(ApiResponses.success(service.regenerateQr(locationId), "Đã tạo mã QR mới"));
    }
}
