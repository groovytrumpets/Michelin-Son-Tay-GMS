package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for customer vehicles list.
 * Used in check-in flow to display vehicle selection dropdown.
 */
@Data
public class CustomerVehiclesResponse {
    private Integer customerId;
    private List<VehicleInfo> vehicles;
    
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
