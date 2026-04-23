package com.g42.platform.gms.promotion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCreateDto {
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
}
