package com.g42.platform.gms.warehouse.api.controller.config;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.warehouse.api.dto.response.ItemProfitReportResponse;
import com.g42.platform.gms.warehouse.app.service.report.WarehouseReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/warehouse/reports")
@RequiredArgsConstructor
public class WarehouseReportController {

    private final WarehouseReportService warehouseReportService;

    @GetMapping("/profit-by-item")
    @PreAuthorize("hasAuthority('" + PermissionCodes.WAREHOUSE_REPORT_VIEW + "')")
    public ResponseEntity<ApiResponse<List<ItemProfitReportResponse>>> getProfitByItem(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Integer warehouseId) {
        return ResponseEntity.ok(ApiResponses.success(
                warehouseReportService.getProfitByItem(fromDate, toDate, warehouseId)));
    }
}
