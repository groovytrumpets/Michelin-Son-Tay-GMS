package com.g42.platform.gms.warehouse.api.controller.config;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.response.CommissionReportResponse;
import com.g42.platform.gms.warehouse.app.service.commission.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouse/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<CommissionReportResponse>>> getReport(
            @RequestParam String periodMonth,
            @RequestParam(required = false) Integer staffId) {
        return ResponseEntity.ok(ApiResponses.success(
                commissionService.getCommissionReport(periodMonth, staffId)));
    }
}
