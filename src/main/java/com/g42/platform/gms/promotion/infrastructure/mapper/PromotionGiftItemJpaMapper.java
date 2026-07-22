package com.g42.platform.gms.promotion.infrastructure.mapper;

import com.g42.platform.gms.promotion.domain.entity.PromotionGiftItem;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionGiftItemJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromotionGiftItemJpaMapper {
    PromotionGiftItemJpa fromDomain(PromotionGiftItem promotion);

    PromotionGiftItem toDomain(PromotionGiftItemJpa promotionGiftItemJpa);
}
