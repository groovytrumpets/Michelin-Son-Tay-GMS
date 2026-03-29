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
@Table(name = "item_category", schema = "michelin_garage")
public class ItemCategoryJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_category_id", nullable = false)
    private Integer itemCategoryId;

    @Size(max = 50)
    @Column(name = "category_code", length = 50)
    private String categoryCode;

    @Size(max = 100)
    @NotNull
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    @Lob
    @Column(name = "category_type")
    private String categoryType;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Byte isActive;


}