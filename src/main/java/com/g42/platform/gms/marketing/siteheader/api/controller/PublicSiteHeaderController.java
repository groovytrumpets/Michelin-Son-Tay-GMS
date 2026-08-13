package com.g42.platform.gms.marketing.siteheader.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.siteheader.app.SiteHeaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bố cục thanh đầu trang cho trang khách — không cần đăng nhập. */
@RestController
@RequestMapping("/api/public/site-header")
@RequiredArgsConstructor
public class PublicSiteHeaderController {

    private final SiteHeaderService siteHeaderService;

    /** Chưa cấu hình gì thì trả data rỗng; giao diện tự dùng bố cục mặc định. */
    @GetMapping
    public ResponseEntity<ApiResponse<JsonNode>> getConfig() {
        return ResponseEntity.ok(ApiResponses.success(
                siteHeaderService.getConfig(SiteHeaderService.LOCATION_HEADER_MAIN)));
    }
}
