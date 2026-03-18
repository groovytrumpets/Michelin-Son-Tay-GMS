package com.g42.platform.gms.estimation.infrastructure.entity;

import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "estimate_item", schema = "michelin_garage")
public class EstimateItemJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estimate_item_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "estimate_id", nullable = false)
    private Integer estimateId;

    @Size(max = 255)
    @Column(name = "item_name")
    private String itemName;


    @Column(name = "item_id")
    private Integer itemId;


    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @ColumnDefault("0")
    @Column(name = "is_overridden")
    private Boolean isOverridden;

    @Size(max = 255)
    @Column(name = "override_reason")
    private String overrideReason;

    @Column(name = "warehouse_id")
    private Integer warehouseId;
    @NotNull
    @Column(name = "work_category_idwork_category", nullable = false)
    private Integer workCategoryId;
    @NotNull

    @Column(name = "tax_rule_idtax_rule", nullable = false)
    private Integer taxRuleId;
    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;
    @ColumnDefault("0")
    @Column(name = "is_checked")
    private Boolean isChecked;


}