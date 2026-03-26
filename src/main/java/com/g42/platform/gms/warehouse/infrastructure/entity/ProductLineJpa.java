package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "product_line", schema = "michelin_garage")
public class ProductLineJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_line_id", nullable = false)
    private Integer productLineId;

    @NotNull
    @Column(name = "brand_id", nullable = false)
    private Integer brandId;

    @Size(max = 100)
    @Column(name = "line_name", length = 100)
    private String lineName;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Byte isActive;


}