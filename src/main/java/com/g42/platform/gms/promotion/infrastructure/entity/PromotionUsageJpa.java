package com.g42.platform.gms.promotion.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "promotion_usage", schema = "michelin_garage")
public class PromotionUsageJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id", nullable = false)
    private Integer promotionUsageId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private PromotionJpa promotion;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "estimate_id")
    private Integer estimateId;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "used_at")
    private Instant usedAt;


}