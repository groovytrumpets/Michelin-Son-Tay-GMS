package com.g42.platform.gms.promotion.infrastructure;

import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.promotion.api.dto.PromotionCreateDto;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.repository.PromotionRepo;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import com.g42.platform.gms.promotion.infrastructure.mapper.PromotionJpaMapper;
import com.g42.platform.gms.promotion.infrastructure.repository.PromotionJpaRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@AllArgsConstructor
public class PromotionRepoImpl implements PromotionRepo {
    @Autowired
    PromotionJpaRepo promotionJpaRepo;
    @Autowired
    PromotionJpaMapper promotionJpaMapper;
    @Override
    public Promotion createNewPromotion(Promotion promotionCreateDto) {
        PromotionJpa promotionJpa = promotionJpaRepo.save(promotionJpaMapper.fromDomain(promotionCreateDto));
        return promotionJpaMapper.toDomain(promotionJpa);
    }

    @Override
    public List<Promotion> getAllPromotion() {
        List<PromotionJpa> promotionJpa = promotionJpaRepo.findAll();
        return promotionJpa.stream().map(promotionJpaMapper::toDomain).toList();
    }

    @Override
    public List<Promotion> getAllPromotionForBilling(ServiceBillDto serviceBillDto) {
        List<PromotionJpa> promotionJpa = promotionJpaRepo.findPromotionOfBilling(LocalDate.now(),serviceBillDto.getSubTotal());
        return promotionJpa.stream().map(promotionJpaMapper::toDomain).toList();
    }
}
