package com.g42.platform.gms.warehouse.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehousePricing {
    private Integer pricingId;
    private Integer warehouseId;
    private Integer itemId;
    private BigDecimal basePrice;
    private BigDecimal markupMultiplier;
    private BigDecimal sellingPrice;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private Instant createdAt;


}