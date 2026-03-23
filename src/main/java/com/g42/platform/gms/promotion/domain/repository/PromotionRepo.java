package com.g42.platform.gms.promotion.domain.repository;

import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.promotion.api.dto.PromotionCreateDto;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepo {
    Promotion createNewPromotion(Promotion promotionCreateDto);

    List<Promotion> getAllPromotion();

    List<Promotion> getAllPromotionForBilling(ServiceBillDto serviceBillDto);
}
