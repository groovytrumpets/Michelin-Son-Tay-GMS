package com.g42.platform.gms.staff.attendance.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.manager.attendance.api.dto.AttendanceCheckinResponse;
import com.g42.platform.gms.staff.attendance.api.dto.QrAttendanceRequest;
import com.g42.platform.gms.staff.attendance.api.dto.QrStatusResponse;
import com.g42.platform.gms.staff.attendance.application.service.StaffAttendanceQrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/attendance")
@RequiredArgsConstructor
public class StaffAttendanceQrController {

    private final StaffAttendanceQrService service;

    /**
     * GET /api/staff/attendance/qr-status?token=
     * Tra cứu vị trí theo mã QR đã quét + trạng thái chấm công hôm nay của bản thân.
     */
    @GetMapping("/qr-status")
    public ResponseEntity<ApiResponse<QrStatusResponse>> getStatus(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestParam String token) {
        return ResponseEntity.ok(ApiResponses.success(service.getStatus(principal.getStaffId(), token)));
    }

    /**
     * POST /api/staff/attendance/qr-check-in
     * Tự chấm công vào bằng mã QR + tọa độ GPS.
     */
    @PostMapping("/qr-check-in")
    public ResponseEntity<ApiResponse<AttendanceCheckinResponse>> checkIn(
            @AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody QrAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.checkIn(principal.getStaffId(), request), "Check-in thành công"));
    }

    /**
     * POST /api/staff/attendance/qr-check-out
     * Tự chấm công ra bằng mã QR + tọa độ GPS.
     */
    @PostMapping("/qr-check-out")
    public ResponseEntity<ApiResponse<AttendanceCheckinResponse>> checkOut(
            @AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody QrAttendanceRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.checkOut(principal.getStaffId(), request), "Check-out thành công"));
    }
}
