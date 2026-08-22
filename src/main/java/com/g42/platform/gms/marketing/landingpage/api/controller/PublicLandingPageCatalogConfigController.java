package com.g42.platform.gms.marketing.landingpage.api.controller;

import com.g42.platform.gms.marketing.landingpage.api.dto.LandingPageConfigDto;
import com.g42.platform.gms.marketing.landingpage.app.service.LandingPageCatalogConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/landing-page-catalog")
public class PublicLandingPageCatalogConfigController {
    private final LandingPageCatalogConfigService configService;

    public PublicLandingPageCatalogConfigController(LandingPageCatalogConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ResponseEntity<LandingPageConfigDto> getConfiguration() {
        return ResponseEntity.ok(configService.getConfiguration());
    }
}
