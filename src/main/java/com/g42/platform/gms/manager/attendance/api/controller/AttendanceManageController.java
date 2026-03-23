package com.g42.platform.gms.manager.attendance.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.manager.attendance.api.dto.AttendanceCheckinResponse;
import com.g42.platform.gms.manager.attendance.api.dto.CheckinRequest;
import com.g42.platform.gms.manager.attendance.api.dto.CheckoutRequest;
import com.g42.platform.gms.manager.attendance.api.dto.TodaySummaryResponse;
import com.g42.platform.gms.manager.attendance.application.service.AttendanceManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager/attendance")
@RequiredArgsConstructor
public class AttendanceManageController {

    private final AttendanceManageService service;

    /**
     * Lấy danh sách điểm danh theo khoảng ngày, có thể filter theo staffId
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADVISOR')")
    public ResponseEntity<ApiResponse<List<AttendanceCheckinResponse>>> getAttendance(
            @RequestParam(required = false) Integer staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponses.success(service.getAttendance(staffId, from, to)));
    }

    /**
     * Lấy điểm danh theo ngày cụ thể (mặc định hôm nay)
     */
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADVISOR')")
    public ResponseEntity<ApiResponse<List<AttendanceCheckinResponse>>> getToday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponses.success(service.getAttendanceByDate(target)));
    }

    /**
     * Tổng hợp điểm danh hôm nay — ai đã check-in, ai chưa
     */
    @GetMapping("/today-summary")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADVISOR')")
    public ResponseEntity<ApiResponse<TodaySummaryResponse>> getTodaySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponses.success(service.getTodaySummary(date)));
    }

    /**
     * Check-in nhân viên vào ca
     */
    @PostMapping("/check-in")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceCheckinResponse>> checkIn(
            @Valid @RequestBody CheckinRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.checkIn(request)));
    }

    /**
     * Check-out nhân viên
     */
    @PutMapping("/{checkinId}/check-out")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<AttendanceCheckinResponse>> checkOut(
            @PathVariable Integer checkinId,
            @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.checkOut(checkinId, request)));
    }

    /**
     * Xóa bản ghi điểm danh (sửa nhầm)
     */
    @DeleteMapping("/{checkinId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteCheckin(@PathVariable Integer checkinId) {
        service.deleteCheckin(checkinId);
        return ResponseEntity.ok(ApiResponses.success("Đã xóa bản ghi điểm danh"));
    }
}
