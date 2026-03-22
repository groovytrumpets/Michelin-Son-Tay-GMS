package com.g42.platform.gms.promotion.app.service;

import com.g42.platform.gms.promotion.api.dto.PromotionCreateDto;
import com.g42.platform.gms.promotion.api.mapper.PromotionDtoMapper;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.exception.PromotionErrorCode;
import com.g42.platform.gms.promotion.domain.exception.PromotionException;
import com.g42.platform.gms.promotion.domain.repository.PromotionRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class PromotionService {
    @Autowired
    PromotionRepo promotionRepo;
    @Autowired
    PromotionDtoMapper promotionDtoMapper;
    @Transactional
    public PromotionCreateDto createNewPromotion(PromotionCreateDto promotionCreateDto) {
        if (promotionCreateDto.getApplyTo() == null ||  (!promotionCreateDto.getApplyTo().equals("ALL") && !promotionCreateDto.getApplyTo().equals("SPECIFIC")) ) {
        throw new PromotionException("INVALID_ENUM_APPLY_TO, must be ALL or SPECIFIC", PromotionErrorCode.INVALID_ENUM_APPLY_TO);
        }
        String type = promotionCreateDto.getType();
        if (type == null
                || (!"PERCENT".equals(type) && !"BUY_X_GET_Y".equals(type))) {
            throw new PromotionException(
                    "INVALID_ENUM_TYPE, must be PERCENT or BUY_X_GET_Y",
                    PromotionErrorCode.INVALID_ENUM_TYPE
            );
        }
        String targetType = promotionCreateDto.getTargetType();
        if (targetType == null
                || (!"ALL".equals(targetType) && !"SPECIFIC".equals(targetType))) {
            throw new PromotionException(
                    "INVALID_ENUM_TARGET_TYPE, must be ALL or SPECIFIC",
                    PromotionErrorCode.INVALID_ENUM_TARGET_TYPE
            );
        }
        Promotion promotion = promotionRepo.createNewPromotion(promotionDtoMapper.fromDto(promotionCreateDto));
        return promotionDtoMapper.toDto(promotion);
    }
}
