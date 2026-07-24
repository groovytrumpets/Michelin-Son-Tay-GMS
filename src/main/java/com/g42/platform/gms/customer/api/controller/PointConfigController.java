package com.g42.platform.gms.customer.api.controller;

import com.g42.platform.gms.customer.api.dto.PointConfigDto;
import com.g42.platform.gms.customer.application.service.PointConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/point-config")
@RequiredArgsConstructor
public class PointConfigController {

    private final PointConfigService pointConfigService;

    @GetMapping
    public ResponseEntity<PointConfigDto> getConfig() {
        return ResponseEntity.ok(pointConfigService.getConfig());
    }

    @PutMapping
    public ResponseEntity<PointConfigDto> updateConfig(@RequestBody PointConfigDto dto) {
        return ResponseEntity.ok(pointConfigService.updateConfig(dto));
    }
}
