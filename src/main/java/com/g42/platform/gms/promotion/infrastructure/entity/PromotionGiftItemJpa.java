package com.g42.platform.gms.promotion.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promotion_gift_item", schema = "michelin_garage")
public class PromotionGiftItemJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_gift_item_id", nullable = false)
    private Integer promotionGiftItemId;

    @NotNull
    @Column(name = "catalog_item_id", nullable = false)
    private Integer catalogItemId;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private PromotionJpa promotion;


}
