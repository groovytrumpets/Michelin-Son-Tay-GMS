package com.g42.platform.gms.promotion.infrastructure.repository;

import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionJpaRepo extends JpaRepository<PromotionJpa,Integer> {
}
