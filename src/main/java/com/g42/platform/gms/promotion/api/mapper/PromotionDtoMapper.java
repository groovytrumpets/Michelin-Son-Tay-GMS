package com.g42.platform.gms.promotion.api.mapper;

import com.g42.platform.gms.promotion.api.dto.PromotionCreateDto;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromotionDtoMapper {
    PromotionCreateDto toDto(Promotion promotion);
    Promotion fromDto(PromotionCreateDto promotionCreateDto);
}
