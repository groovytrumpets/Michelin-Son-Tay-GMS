package com.g42.platform.gms.dashboard.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.dashboard.api.dto.StaffDashboardConfigRequest;
import com.g42.platform.gms.dashboard.application.service.StaffDashboardConfigService;
import com.g42.platform.gms.dashboard.infrastructure.entity.StaffDashboardConfigJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/dashboard/configs")
@RequiredArgsConstructor
public class StaffDashboardConfigController {

    private final StaffDashboardConfigService configService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffDashboardConfigJpa>>> getAllConfigs(
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(configService.getAllConfigs(principal.getStaffId())));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<StaffDashboardConfigJpa>> getActiveConfig(
            @AuthenticationPrincipal StaffPrincipal principal) {
        return ResponseEntity.ok(ApiResponses.success(configService.getActiveConfig(principal.getStaffId()).orElse(null)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffDashboardConfigJpa>> createConfig(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestBody StaffDashboardConfigRequest request) {
        return ResponseEntity.ok(ApiResponses.success(configService.createConfig(principal.getStaffId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StaffDashboardConfigJpa>> updateConfig(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer id,
            @RequestBody StaffDashboardConfigRequest request) {
        return ResponseEntity.ok(ApiResponses.success(configService.updateConfig(principal.getStaffId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConfig(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer id) {
        configService.deleteConfig(principal.getStaffId(), id);
        return ResponseEntity.ok(ApiResponses.success(null));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<StaffDashboardConfigJpa>> activateConfig(
            @AuthenticationPrincipal StaffPrincipal principal,
            @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponses.success(configService.activateConfig(principal.getStaffId(), id)));
    }
}
