package com.g42.platform.gms.manager.schedule.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.manager.schedule.dto.*;
import com.g42.platform.gms.manager.schedule.service.WorkShiftManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager/work-shifts")
@RequiredArgsConstructor
public class WorkShiftManageController {

    private final WorkShiftManageService service;

    // ===== Stats =====

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADVISOR')")
    public ResponseEntity<ApiResponse<WorkShiftStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponses.success(service.getStats()));
    }

    // ===== Work Shift definitions =====

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADVISOR')")
    public ResponseEntity<ApiResponse<List<WorkShiftResponse>>> getAllShifts() {
        return ResponseEntity.ok(ApiResponses.success(service.getAllShifts()));
    }

    @GetMapping("/{shiftId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADVISOR')")
    public ResponseEntity<ApiResponse<WorkShiftResponse>> getShift(@PathVariable Integer shiftId) {
        return ResponseEntity.ok(ApiResponses.success(service.getShiftById(shiftId)));
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<WorkShiftResponse>> createShift(
            @Valid @RequestBody WorkShiftRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.createShift(request)));
    }

    @PutMapping("/{shiftId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<WorkShiftResponse>> updateShift(
            @PathVariable Integer shiftId,
            @Valid @RequestBody WorkShiftRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.updateShift(shiftId, request)));
    }

    @DeleteMapping("/{shiftId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteShift(@PathVariable Integer shiftId) {
        service.deleteShift(shiftId);
        return ResponseEntity.ok(ApiResponses.success("Đã vô hiệu hóa ca làm việc"));
    }

    // ===== Staff Schedule (phân công) =====

    @GetMapping("/schedules")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADVISOR')")
    public ResponseEntity<ApiResponse<List<StaffScheduleResponse>>> getSchedules(
            @RequestParam(required = false) Integer staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponses.success(service.getSchedules(staffId, from, to)));
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<StaffScheduleResponse>> createSchedule(
            @Valid @RequestBody StaffScheduleRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.createSchedule(request)));
    }

    @PutMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<StaffScheduleResponse>> updateSchedule(
            @PathVariable Integer scheduleId,
            @Valid @RequestBody StaffScheduleRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.updateSchedule(scheduleId, request)));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteSchedule(@PathVariable Integer scheduleId) {
        service.deleteSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponses.success("Đã hủy lịch làm việc"));
    }
}
