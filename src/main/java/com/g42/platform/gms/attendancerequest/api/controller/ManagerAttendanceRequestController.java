package com.g42.platform.gms.attendancerequest.api.controller;

import com.g42.platform.gms.attendancerequest.api.dto.AttendanceRequestResponse;
import com.g42.platform.gms.attendancerequest.api.dto.AttendanceRequestReviewRequest;
import com.g42.platform.gms.attendancerequest.application.service.AttendanceRequestService;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/attendance-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ManagerAttendanceRequestController {

    private final AttendanceRequestService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AttendanceRequestResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponses.success(service.listForManager(status, type)));
    }

    @PutMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<AttendanceRequestResponse>> approve(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer requestId,
            @RequestBody(required = false) AttendanceRequestReviewRequest request) {
        String reviewNote = request != null ? request.getReviewNote() : null;
        return ResponseEntity.ok(ApiResponses.success(
                service.approve(principal.getStaffId(), requestId, reviewNote), "Đã duyệt yêu cầu"));
    }

    @PutMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<AttendanceRequestResponse>> reject(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer requestId,
            @RequestBody AttendanceRequestReviewRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                service.reject(principal.getStaffId(), requestId, request.getReviewNote()), "Đã từ chối yêu cầu"));
    }
}
