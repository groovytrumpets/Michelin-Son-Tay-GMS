package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "brand", schema = "michelin_garage")
public class BrandJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Column(name = "brand_name", length = 100)
    private String brandName;

    @Size(max = 255)
    @Column(name = "logo_url")
    private String logoUrl;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Byte isActive;


}