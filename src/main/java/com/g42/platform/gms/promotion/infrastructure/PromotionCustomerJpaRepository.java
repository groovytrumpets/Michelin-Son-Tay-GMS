package com.g42.platform.gms.promotion.infrastructure;

import com.g42.platform.gms.promotion.domain.entity.PromotionCustomer;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionCustomerJpa;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface PromotionCustomerJpaRepository extends JpaRepository<PromotionCustomerJpa, Integer> {
    List<PromotionCustomerJpa> getAllByPromotion(PromotionJpa promotion);

    void deleteByPromotion(PromotionJpa promotion);
}
