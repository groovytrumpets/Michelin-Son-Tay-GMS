package com.g42.platform.gms.promotion.domain.repository;

import com.g42.platform.gms.promotion.api.dto.PromotionCreateDto;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepo {
    Promotion createNewPromotion(Promotion promotionCreateDto);
}
