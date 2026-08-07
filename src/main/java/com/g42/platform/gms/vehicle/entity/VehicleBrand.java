package com.g42.platform.gms.vehicle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Hãng xe (Toyota, Honda, VinFast...). Tách riêng khỏi bảng {@code brand} vì
 * bảng đó là thương hiệu phụ tùng (Michelin, Castrol...).
 */
@Entity
@Table(name = "vehicle_brand")
@Getter
@Setter
public class VehicleBrand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
