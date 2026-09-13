package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "product_unit", schema = "michelin_garage")
public class ProductUnitJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id", nullable = false)
    private Integer unitId;

    @Size(max = 50)
    @Column(name = "unit_name", length = 50, nullable = false, unique = true)
    private String unitName;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Byte isActive = 1;

    /** Kiểu đo lường gợi ý cho sản phẩm chọn đơn vị này: COUNT hoặc MEASURE. */
    @Column(name = "measurement_type", length = 10, nullable = false)
    private String measurementType = "COUNT";

    @Column(name = "decimal_scale", nullable = false)
    private Integer decimalScale = 0;
}
