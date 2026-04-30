package com.g42.platform.gms.promotion.infrastructure.repository;

import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PromotionJpaRepo extends JpaRepository<PromotionJpa,Integer> {
    @Query("""
    select p from PromotionJpa p 
        where p.promotionId =:promotionId
            and p.isActive = true 
            and p.startDate <= :now 
            and (p.endDate is null or p.endDate>=:now)
            and (p.minOrderValue is null or p.minOrderValue <= :subTotal)
            and (p.usageLimit is null or p.usedCount<p.usageLimit)
    """)
    PromotionJpa findPromotionOfBilling(LocalDate now, BigDecimal subTotal, Integer promotionId);
    @Query("""
    select distinct p from PromotionJpa p left join PromotionCustomerJpa pc on p=pc.promotion
            where p.type=:promotionType 
                and(p.targetType = 'ALL' or (p.targetType='SPECIFIC' and pc.customerProfileId = :customerId) )
    """)
    List<PromotionJpa> findAllAvailable(String promotionType, String customerId);

    PromotionJpa findPromotionJpasByCode(String code);

    PromotionJpa findByCode(String code);
}
