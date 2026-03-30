package com.g42.platform.gms.promotion.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionUsage {
    private Integer promotionUsageId;
    private Promotion promotion;
    private Integer customerId;
    private Integer estimateId;
    private BigDecimal discountAmount;
    private Instant usedAt;


}