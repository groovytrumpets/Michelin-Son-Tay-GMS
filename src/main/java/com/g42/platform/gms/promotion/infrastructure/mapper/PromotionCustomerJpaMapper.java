package com.g42.platform.gms.promotion.infrastructure.mapper;

import com.g42.platform.gms.promotion.domain.entity.PromotionCustomer;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionCustomerJpa;
import org.mapstruct.Mapper;

@Mapper (componentModel = "spring")
public interface PromotionCustomerJpaMapper {

    PromotionCustomer toDomain(PromotionCustomerJpa promotionCustomerJpa);
}
