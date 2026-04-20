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
    public Promotion getAllPromotionForBilling(ServiceBillDto serviceBillDto) {
        PromotionJpa promotionJpa = promotionJpaRepo.
                findPromotionOfBilling(LocalDate.now(),serviceBillDto.getSubTotal(),serviceBillDto.getPromotionId());
        return promotionJpaMapper.toDomain(promotionJpa);
    }

    @Override
    public List<Promotion> getAllAvailablePromotion() {
        List<PromotionJpa> promotionJpa = promotionJpaRepo.findAllAvailable();
        return promotionJpa.stream().map(promotionJpaMapper::toDomain).toList();
    }

    @Override
    public Promotion getPromotionByCode(String code) {
        return promotionJpaMapper.toDomain(promotionJpaRepo.findPromotionJpasByCode(code));
    }

    @Override
    public Promotion updatePromotion(Integer promotionId, Promotion promotion) {

        PromotionJpa promotionJpa = new PromotionJpa();
        promotionJpa.setPromotionId(promotionId);
        PromotionJpa saved = promotionJpaRepo.save(promotionJpa);
        return promotionJpaMapper.toDomain(saved);
    }

    @Override
    public void countUsed(Integer promotionId) {
        PromotionJpa promotionJpa = promotionJpaRepo.findById(promotionId).orElse(null);
        if (promotionJpa == null) {
            System.err.println("PROMOTION UPDATE USEDCOUNT NOT FOUND");
            return;
        }
        promotionJpa.setUsedCount(promotionJpa.getUsedCount() + 1);
        promotionJpaRepo.save(promotionJpa);
    }
}
