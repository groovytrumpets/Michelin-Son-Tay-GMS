package com.g42.platform.gms.vehicle.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO for listing customer vehicles.
 * Returns all vehicles owned by a customer.
 */
@Data
public class VehicleListResponse {
    
    private Integer customerId;
    private List<VehicleInfo> vehicles = new ArrayList<>();
    
    @Data
    public static class VehicleInfo {
        private Integer vehicleId;
        private String licensePlate;
        private String make;
        private String model;
        private Integer year;
        private Integer lastOdometerReading;
        private LocalDate lastServiceDate;
    }
}
