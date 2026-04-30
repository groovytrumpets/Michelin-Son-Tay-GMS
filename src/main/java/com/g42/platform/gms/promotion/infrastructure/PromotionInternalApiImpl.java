package com.g42.platform.gms.promotion.infrastructure;

import com.g42.platform.gms.promotion.api.internal.PromotionInternalApi;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.entity.PromotionItem;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import com.g42.platform.gms.promotion.infrastructure.mapper.PromotionCustomerJpaMapper;
import com.g42.platform.gms.promotion.infrastructure.mapper.PromotionItemJpaMapper;
import com.g42.platform.gms.promotion.infrastructure.mapper.PromotionJpaMapper;
import com.g42.platform.gms.promotion.infrastructure.repository.PromotionJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PromotionInternalApiImpl implements PromotionInternalApi {
    @Autowired
    PromotionJpaRepo promotionJpaRepo;
    @Autowired
    PromotionJpaMapper promotionJpaMapper;
    @Autowired
    private PromotionItemJpaRepository promotionItemJpaRepository;
    @Autowired
    private PromotionCustomerJpaRepository promotionCustomerJpaRepository;
    @Autowired
    private PromotionItemJpaMapper promotionItemJpaMapper;
    @Autowired
    private PromotionCustomerJpaMapper promotionCustomerJpaMapper;
    @Autowired
    private PromotionRepoImpl promotionRepo;

    @Override
    public Promotion findById(Integer promotionId) {
        return promotionJpaMapper.toDomain(promotionJpaRepo.findById(promotionId).orElse(null));
    }

    @Override
    public Promotion findByPromotionCode(String promotionCode) {
        return promotionJpaMapper.toDomain(promotionJpaRepo.findByCode(promotionCode));
    }

    @Override
    public List<Integer> findItemIdsByPromotionId(Promotion promotionId) {
        return promotionRepo.findPromotionItemById(promotionId).stream().map(PromotionItem::getCatalogItemId).toList();
    }

    @Override
    public void initializePromotionCount(Promotion promotion) {
        PromotionJpa promotionJpa = promotionJpaRepo.findById(promotion.getPromotionId()).get();
        promotionJpa.setUsedCount(0);
        promotionJpaRepo.save(promotionJpa);
    }

    @Override
    public int incrementUsedCountIfAvailable(Integer promotionId) {
        return promotionJpaRepo.incrementUsedCountIfAvailable(promotionId);
    }

    @Override
    public void savePromotion(Promotion promotion) {
        promotionJpaRepo.save(promotionJpaMapper.toJpa(promotion));
    }
}
