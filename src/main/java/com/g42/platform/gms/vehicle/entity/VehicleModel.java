package com.g42.platform.gms.vehicle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Dòng xe thuộc một hãng (Camry, Civic, VF 8...).
 */
@Entity
@Table(name = "vehicle_model")
@Getter
@Setter
public class VehicleModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Integer modelId;

    @Column(name = "brand_id", nullable = false)
    private Integer brandId;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
