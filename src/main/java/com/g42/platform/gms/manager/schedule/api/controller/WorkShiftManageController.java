package com.g42.platform.gms.manager.schedule.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.manager.schedule.api.dto.WorkShiftRequest;
import com.g42.platform.gms.manager.schedule.api.dto.WorkShiftResponse;
import com.g42.platform.gms.manager.schedule.application.service.WorkShiftManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/work-shifts")
@RequiredArgsConstructor
public class WorkShiftManageController {

    private final WorkShiftManageService service;

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
    public ResponseEntity<ApiResponse<WorkShiftResponse>> createShift(@Valid @RequestBody WorkShiftRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.createShift(request)));
    }

    @PutMapping("/{shiftId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<WorkShiftResponse>> updateShift(
            @PathVariable Integer shiftId, @Valid @RequestBody WorkShiftRequest request) {
        return ResponseEntity.ok(ApiResponses.success(service.updateShift(shiftId, request)));
    }

    @DeleteMapping("/{shiftId}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<String>> deleteShift(@PathVariable Integer shiftId) {
        service.deleteShift(shiftId);
        return ResponseEntity.ok(ApiResponses.success("Đã vô hiệu hóa ca làm việc"));
    }
}
