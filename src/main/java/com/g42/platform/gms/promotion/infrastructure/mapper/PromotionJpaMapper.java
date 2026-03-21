package com.g42.platform.gms.promotion.infrastructure.mapper;

import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromotionJpaMapper {
    Promotion toDomain(PromotionJpa domain);
    PromotionJpa fromDomain(Promotion domain);
}
