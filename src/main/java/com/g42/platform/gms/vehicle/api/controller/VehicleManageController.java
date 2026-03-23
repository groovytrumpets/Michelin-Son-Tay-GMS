package com.g42.platform.gms.vehicle.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.vehicle.api.dto.CreateVehicleRequest;
import com.g42.platform.gms.vehicle.api.dto.UpdateVehicleRequest;
import com.g42.platform.gms.vehicle.api.dto.VehicleListResponse;
import com.g42.platform.gms.vehicle.api.dto.VehicleResponse;
import com.g42.platform.gms.vehicle.application.service.VehicleManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleManageController {

    private final VehicleManageService vehicleManageService;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<VehicleListResponse>> getCustomerVehicles(@PathVariable Integer customerId) {
        return ResponseEntity.ok(ApiResponses.success(vehicleManageService.getCustomerVehicles(customerId)));
    }

    @GetMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleDetail(@PathVariable Integer vehicleId) {
        return ResponseEntity.ok(ApiResponses.success(vehicleManageService.getVehicleDetail(vehicleId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(@Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponses.success(vehicleManageService.createVehicle(request)));
    }

    @PutMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable Integer vehicleId, @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(ApiResponses.success(vehicleManageService.updateVehicle(vehicleId, request)));
    }

    @DeleteMapping("/{vehicleId}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(@PathVariable Integer vehicleId) {
        vehicleManageService.deleteVehicle(vehicleId);
        return ResponseEntity.ok(ApiResponses.success(null));
    }
}
