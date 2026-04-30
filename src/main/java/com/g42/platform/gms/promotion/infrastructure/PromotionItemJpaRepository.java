package com.g42.platform.gms.promotion.infrastructure;

import com.g42.platform.gms.promotion.domain.entity.PromotionItem;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionItemJpa;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PromotionItemJpaRepository extends JpaRepository<PromotionItemJpa, Integer> {
    List<PromotionItemJpa> getAllByPromotion(PromotionJpa promotion);

    void deleteByPromotion(PromotionJpa promotion);
}
