package com.g42.platform.gms.vehicle.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.vehicle.dto.VehicleListResponse;
import com.g42.platform.gms.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for vehicle operations.
 * Handles vehicle-related API endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    /**
     * Get all vehicles owned by a customer.
     * Returns list of vehicles with last odometer reading and last service date.
     * 
     * GET /api/vehicles/customer/{customerId}
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<VehicleListResponse>> getCustomerVehicles(@PathVariable Integer customerId) {
        try {
            log.info("Getting vehicles for customer: {}", customerId);
            VehicleListResponse response = vehicleService.getCustomerVehicles(customerId);
            return ResponseEntity.ok(ApiResponses.success(response));
        } catch (RuntimeException e) {
            log.error("Get customer vehicles failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponses.error("CUSTOMER_NOT_FOUND", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error getting customer vehicles", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponses.error("INTERNAL_ERROR", "Lỗi hệ thống"));
        }
    }
}
