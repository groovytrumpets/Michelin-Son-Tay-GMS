package com.g42.platform.gms.promotion.api.internal;

import com.g42.platform.gms.promotion.domain.entity.Promotion;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public interface PromotionInternalApi {
    Promotion findById(Integer promotionId);

    Promotion findByPromotionCode(String promotionCode);

    List<Integer> findItemIdsByPromotionId(Promotion promotionId);

    void initializePromotionCount(Promotion promotion);
}
