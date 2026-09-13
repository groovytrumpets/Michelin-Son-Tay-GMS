package com.g42.platform.gms.vehicle.dto;

import lombok.Data;

/**
 * Response DTO after updating a vehicle's info.
 */
@Data
public class VehicleUpdateResponse {

    private Integer vehicleId;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer manufactureYear;
}
