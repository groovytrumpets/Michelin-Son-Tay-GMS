package com.g42.platform.gms.promotion.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promotion_item", schema = "michelin_garage")
public class PromotionItemJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_item_id", nullable = false)
    private Integer promotionItemId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_promotion_id", nullable = false)
    private PromotionJpa promotionPromotion;

    @NotNull
    @Column(name = "catalog_item_item_id", nullable = false)
    private Integer catalogItemId;


}