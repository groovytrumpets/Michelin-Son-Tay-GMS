package com.g42.platform.gms.marketing.siteheader.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.siteheader.app.SiteHeaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cấu hình bố cục thanh đầu trang khách (màn /nav-menu-config). */
@RestController
@RequestMapping("/api/admin/site-header")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class AdminSiteHeaderController {

    private final SiteHeaderService siteHeaderService;

    @GetMapping
    public ResponseEntity<ApiResponse<JsonNode>> getConfig() {
        return ResponseEntity.ok(ApiResponses.success(
                siteHeaderService.getConfig(SiteHeaderService.LOCATION_HEADER_MAIN)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<JsonNode>> saveConfig(
            @AuthenticationPrincipal StaffPrincipal principal,
            @RequestBody JsonNode config) {
        return ResponseEntity.ok(ApiResponses.success(
                siteHeaderService.saveConfig(
                        SiteHeaderService.LOCATION_HEADER_MAIN,
                        config,
                        principal == null ? null : principal.getStaffId()),
                "Đã lưu bố cục thanh đầu trang"));
    }
}
