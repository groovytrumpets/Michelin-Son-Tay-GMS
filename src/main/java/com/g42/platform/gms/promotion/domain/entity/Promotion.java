package com.g42.platform.gms.promotion.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class Promotion {
    private Integer promotionId;
    private String code;
    private String name;
    private String type;
    private BigDecimal discountPercent;
    private Boolean isActive;
    private String applyTo;
    private Integer buyItemId;
    private Integer buyQuantity;
    private Integer getItemId;
    private Integer getQuantity;
    private String targetType;
    private BigDecimal minOrderValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer usageLimit;
    private Integer usedCount;
    private Instant createdAt;


}