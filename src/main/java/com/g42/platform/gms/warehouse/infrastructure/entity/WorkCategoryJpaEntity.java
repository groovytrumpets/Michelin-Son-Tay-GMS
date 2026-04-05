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
@Table(name = "work_category", schema = "michelin_garage")
public class WorkCategoryJpaEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idwork_category", nullable = false)
    private Integer workCategoryId;

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
    @ColumnDefault("0")
    @Column(name = "is_default")
    private Boolean isDefault;
    @NotNull
    @Column(name = "tax_rule_idtax_rule", nullable = false)
    private Integer taxRuleId;
    @Lob
    @Column(name = "category_type")
    private String categoryType;


}