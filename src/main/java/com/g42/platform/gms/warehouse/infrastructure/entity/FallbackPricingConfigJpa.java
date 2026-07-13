package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
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
@Table(name = "fallback_pricing_config")
public class FallbackPricingConfigJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 50)
    private CatalogItemType itemType;

    @NotNull
    @ColumnDefault("1.0000")
    @Column(name = "markup_multiplier", nullable = false, precision = 6, scale = 4)
    private BigDecimal markupMultiplier;

    @NotNull
    @ColumnDefault("1.0000")
    @Column(name = "markup_multiplier_wholesale", nullable = false, precision = 6, scale = 4)
    private BigDecimal markupMultiplierWholesale;

    @Column(name = "description", length = 255)
    private String description;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive = true;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt = Instant.now();
}
