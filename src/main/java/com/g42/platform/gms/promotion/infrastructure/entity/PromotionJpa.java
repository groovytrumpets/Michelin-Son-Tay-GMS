package com.g42.platform.gms.promotion.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "promotion", schema = "michelin_garage")
public class PromotionJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id", nullable = false)
    private Integer promotionId;

    @Size(max = 50)
    @Column(name = "code", length = 50)
    private String code;

    @Size(max = 100)
    @Column(name = "name", length = 100)
    private String name;

    @Lob
    @Column(name = "type")
    private String type;

    @Column(name = "discount_percent", precision = 12, scale = 2)
    private BigDecimal discountPercent;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive;

    @Lob
    @Column(name = "apply_to")
    private String applyTo;

    @Column(name = "buy_item_id")
    private Integer buyItemId;

    @Column(name = "buy_quantity")
    private Integer buyQuantity;

    @Column(name = "get_item_id")
    private Integer getItemId;

    @Column(name = "get_quantity")
    private Integer getQuantity;

    @Lob
    @Column(name = "target_type")
    private String targetType;

    @Column(name = "min_order_value", precision = 12, scale = 2)
    private BigDecimal minOrderValue;

    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @ColumnDefault("0")
    @Column(name = "used_count")
    private Integer usedCount;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;


}