package com.g42.platform.gms.promotion.infrastructure;

import com.g42.platform.gms.promotion.infrastructure.entity.PromotionGiftItemJpa;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PromotionGiftItemJpaRepository extends JpaRepository<PromotionGiftItemJpa, Integer> {
    List<PromotionGiftItemJpa> getAllByPromotion(PromotionJpa promotion);

    void deleteByPromotion(PromotionJpa promotion);
}
