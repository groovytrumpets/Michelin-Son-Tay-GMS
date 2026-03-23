package com.g42.platform.gms.vehicle.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

/**
 * JPA entity for vehicle table.
 */
@Entity(name = "VehicleManagement")
@Table(name = "vehicle")
@Data
public class VehicleJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Integer vehicleId;

    @Column(name = "license_plate", nullable = false, unique = true)
    private String licensePlate;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "manufacture_year")
    private Integer manufactureYear;

    @Column(name = "customer_id")
    private Integer customerId;


}
