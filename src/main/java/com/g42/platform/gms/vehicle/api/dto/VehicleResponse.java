package com.g42.platform.gms.vehicle.api.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * Response DTO for a single vehicle with enriched info.
 */
@Data
public class VehicleResponse {

    private Integer vehicleId;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer manufactureYear;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private Integer lastOdometerReading;
    private LocalDate lastServiceDate;
}
