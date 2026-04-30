package com.g42.platform.gms.promotion.infrastructure.mapper;

import com.g42.platform.gms.promotion.domain.entity.PromotionItem;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionItemJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromotionItemJpaMapper {
    PromotionItemJpa fromDomain(PromotionItem promotion);

    PromotionItem toDomain(PromotionItemJpa promotionItemJpa);
}
