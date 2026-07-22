package com.g42.platform.gms.promotion.infrastructure.mapper;

import com.g42.platform.gms.promotion.domain.entity.PromotionBuyItem;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionBuyItemJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromotionBuyItemJpaMapper {
    PromotionBuyItemJpa fromDomain(PromotionBuyItem promotion);

    PromotionBuyItem toDomain(PromotionBuyItemJpa promotionBuyItemJpa);
}
