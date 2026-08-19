package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

@Getter
@Setter
@Entity
@Table(name = "item_color")
public class ItemColorJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_color_id", nullable = false)
    private Integer itemColorId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Size(max = 20)
    @Column(name = "color_code", length = 20, nullable = false)
    private String colorCode;

    @Size(max = 100)
    @Column(name = "color_name", length = 100)
    private String colorName;

    @ColumnDefault("0")
    @Column(name = "display_order")
    private Integer displayOrder;
}
