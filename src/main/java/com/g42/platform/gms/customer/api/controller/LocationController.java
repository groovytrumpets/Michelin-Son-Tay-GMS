package com.g42.platform.gms.customer.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.customer.api.dto.LocationDto;
import com.g42.platform.gms.customer.application.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Danh mục địa giới hành chính dùng cho form Danh bạ đối tác.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/locations")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<LocationDto>>> provinces() {
        return ResponseEntity.ok(ApiResponses.success(locationService.getProvinces()));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<LocationDto>>> districts(@RequestParam String provinceId) {
        return ResponseEntity.ok(ApiResponses.success(locationService.getDistricts(provinceId)));
    }

    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<LocationDto>>> wards(@RequestParam String districtId) {
        return ResponseEntity.ok(ApiResponses.success(locationService.getWards(districtId)));
    }
}
