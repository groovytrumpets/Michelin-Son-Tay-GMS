package com.g42.platform.gms.promotion.domain.entity;

import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionGiftItem {
    private Integer promotionGiftItemId;
    private Integer catalogItemId;
    private Integer quantity;
    private PromotionJpa promotion;


}
