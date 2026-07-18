package com.g42.platform.gms.attendancerequest.api.controller;

import com.g42.platform.gms.attendancerequest.api.dto.AttendanceRequestCreateRequest;
import com.g42.platform.gms.attendancerequest.api.dto.AttendanceRequestResponse;
import com.g42.platform.gms.attendancerequest.application.service.AttendanceRequestService;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.manager.schedule.api.dto.WorkShiftResponse;
import com.g42.platform.gms.manager.schedule.domain.entity.WorkShift;
import com.g42.platform.gms.manager.schedule.domain.repository.WorkShiftRepo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/attendance-requests")
@RequiredArgsConstructor
public class StaffAttendanceRequestController {

    private final AttendanceRequestService service;
    private final WorkShiftRepo workShiftRepo;

    /**
     * Danh sách ca làm để chọn khi gửi yêu cầu chấm công bù — mở cho mọi nhân viên
     * đã đăng nhập (khác với /api/manager/work-shifts vốn chỉ dành cho MANAGER/ADVISOR).
     */
    @GetMapping("/shifts")
    public ResponseEntity<ApiResponse<List<WorkShiftResponse>>> listShifts() {
        List<WorkShiftResponse> shifts = workShiftRepo.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .map(this::toShiftResponse)
                .toList();
        return ResponseEntity.ok(ApiResponses.success(shifts));
    }

    private WorkShiftResponse toShiftResponse(WorkShift shift) {
        WorkShiftResponse r = new WorkShiftResponse();
        r.setShiftId(shift.getShiftId());
        r.setShiftName(shift.getShiftName());
        r.setStartTime(shift.getStartTime());
        r.setEndTime(shift.getEndTime());
        r.setIsActive(shift.getIsActive());
        r.setCreatedAt(shift.getCreatedAt());
        return r;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AttendanceRequestResponse>>> listMine(
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(service.listMine(principal.getStaffId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AttendanceRequestResponse>> create(
            @AuthenticationPrincipal StaffPrincipal principal,
            @Valid @RequestBody AttendanceRequestCreateRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                service.create(principal.getStaffId(), request), "Đã gửi yêu cầu, chờ quản lý duyệt"));
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<ApiResponse<String>> cancel(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer requestId) {
        service.cancel(principal.getStaffId(), requestId);
        return ResponseEntity.ok(ApiResponses.success("Đã hủy yêu cầu"));
    }
}
