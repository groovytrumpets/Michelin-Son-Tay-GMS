package com.g42.platform.gms.vehicle.entity;

import com.g42.platform.gms.auth.entity.CustomerProfile; // Import từ Auth
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vehicle")
@Data
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer vehicleId;

    // Biển số xe (Unique)
    @Column(nullable = false, unique = true)
    private String licensePlate;

    private String brand;
    private String model;
    private Integer manufactureYear;

    // Ai là chủ sở hữu xe này?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;
}