package com.g42.platform.gms.promotion.infrastructure;

import com.g42.platform.gms.promotion.infrastructure.entity.PromotionBuyItemJpa;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PromotionBuyItemJpaRepository extends JpaRepository<PromotionBuyItemJpa, Integer> {
    List<PromotionBuyItemJpa> getAllByPromotion(PromotionJpa promotion);

    void deleteByPromotion(PromotionJpa promotion);
}
