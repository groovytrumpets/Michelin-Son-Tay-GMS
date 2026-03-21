package com.g42.platform.gms.promotion.app.service;

import com.g42.platform.gms.promotion.api.dto.PromotionCreateDto;
import com.g42.platform.gms.promotion.api.mapper.PromotionDtoMapper;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.repository.PromotionRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class PromotionService {
    @Autowired
    PromotionRepo promotionRepo;
    @Autowired
    PromotionDtoMapper promotionDtoMapper;

    public PromotionCreateDto createNewPromotion(PromotionCreateDto promotionCreateDto) {
        Promotion promotion = promotionRepo.createNewPromotion(promotionDtoMapper.fromDto(promotionCreateDto));
        return promotionDtoMapper.toDto(promotion);
    }
}
