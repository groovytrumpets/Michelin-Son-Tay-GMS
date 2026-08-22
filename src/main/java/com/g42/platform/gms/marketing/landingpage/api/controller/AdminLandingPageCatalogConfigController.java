package com.g42.platform.gms.marketing.landingpage.api.controller;

import com.g42.platform.gms.marketing.landingpage.api.dto.LandingPageConfigDto;
import com.g42.platform.gms.marketing.landingpage.api.dto.LandingPageConfigUpdateRequest;
import com.g42.platform.gms.marketing.landingpage.app.service.LandingPageCatalogConfigService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/landing-page-catalog")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class AdminLandingPageCatalogConfigController {
    private final LandingPageCatalogConfigService configService;

    public AdminLandingPageCatalogConfigController(LandingPageCatalogConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public ResponseEntity<LandingPageConfigDto> getConfiguration() {
        return ResponseEntity.ok(configService.getConfiguration());
    }

    @PutMapping
    public ResponseEntity<LandingPageConfigDto> updateConfiguration(
            @Valid @RequestBody LandingPageConfigUpdateRequest request
    ) {
        return ResponseEntity.ok(configService.updateConfiguration(request));
    }
}
