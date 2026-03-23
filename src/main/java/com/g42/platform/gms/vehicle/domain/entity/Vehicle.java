package com.g42.platform.gms.vehicle.domain.entity;

import lombok.Data;

/**
 * Domain entity for Vehicle.
 * Pure POJO - no JPA annotations, no framework dependencies.
 */
@Data
public class Vehicle {

    private Integer vehicleId;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer manufactureYear;
    private Integer customerId;
}
