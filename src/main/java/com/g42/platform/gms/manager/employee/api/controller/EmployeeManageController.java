package com.g42.platform.gms.manager.employee.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.manager.employee.api.dto.EmployeeDetailResponse;
import com.g42.platform.gms.manager.employee.api.dto.EmployeeResponse;
import com.g42.platform.gms.manager.employee.application.service.EmployeeManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/employees")
@RequiredArgsConstructor
public class EmployeeManageController {

    private final EmployeeManageService service;

    /**
     * Danh sách tất cả nhân viên
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADVISOR')")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAllEmployees() {
        return ResponseEntity.ok(ApiResponses.success(service.getAllEmployees()));
    }

    /**
     * Chi tiết nhân viên — bao gồm hiệu năng tháng này và lịch điểm danh 30 ngày gần nhất
     */
    @GetMapping("/{staffId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADVISOR')")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> getEmployeeDetail(@PathVariable Integer staffId) {
        return ResponseEntity.ok(ApiResponses.success(service.getEmployeeDetail(staffId)));
    }
}
