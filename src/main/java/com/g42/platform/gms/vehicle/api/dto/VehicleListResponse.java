package com.g42.platform.gms.vehicle.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for listing vehicles of a customer.
 */
@Data
public class VehicleListResponse {

    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private List<VehicleItem> vehicles;

    @Data
    public static class VehicleItem {
        private Integer vehicleId;
        private String licensePlate;
        private String brand;
        private String model;
        private Integer manufactureYear;
        private Integer lastOdometerReading;
        private LocalDate lastServiceDate;
    }
}
