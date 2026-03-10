package com.g42.platform.gms.estimation.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "work_category", schema = "michelin_garage")
public class WorkCategoryJpa {
    @Id
    @Column(name = "idwork_category", nullable = false)
    private Integer id;

    @Size(max = 50)
    @Column(name = "category_code", length = 50)
    private String categoryCode;

    @Size(max = 100)
    @Column(name = "category_name", length = 100)
    private String categoryName;

    @NotNull
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive;



}