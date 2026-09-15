package com.g42.platform.gms.vehicle.entity;

import com.g42.platform.gms.auth.entity.CustomerProfile; // Import từ Auth
import com.g42.platform.gms.vehicle.support.PlateKeys;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "vehicle")
@Data
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer vehicleId;

    // Biển số xe nguyên văn như người dùng nhập
    @Column(nullable = false, unique = true)
    private String licensePlate;

    /**
     * Biển số chuẩn hoá (in hoa, bỏ dấu/khoảng trắng) — khoá DUY NHẤT thật sự của xe
     * (UNIQUE từ changeset 037). Tự tính lại mỗi lần lưu, không set tay.
     */
    @Column(name = "plate_key", length = 32)
    private String plateKey;

    private String brand;
    private String model;
    private Integer manufactureYear;

    // Ai là chủ sở hữu xe này?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @PrePersist
    @PreUpdate
    void syncPlateKey() {
        this.plateKey = PlateKeys.normalize(licensePlate);
    }
}
