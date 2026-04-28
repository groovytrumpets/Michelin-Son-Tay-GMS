package com.g42.platform.gms.promotion.app.service;

import com.g42.platform.gms.promotion.api.dto.PromotionCreateDto;
import com.g42.platform.gms.promotion.api.mapper.PromotionDtoMapper;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.entity.PromotionCustomer;
import com.g42.platform.gms.promotion.domain.entity.PromotionItem;
import com.g42.platform.gms.promotion.domain.exception.PromotionErrorCode;
import com.g42.platform.gms.promotion.domain.exception.PromotionException;
import com.g42.platform.gms.promotion.domain.repository.PromotionRepo;
import io.jsonwebtoken.lang.Assert;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AllArgsConstructor
@Service
public class PromotionService {
    @Autowired
    PromotionRepo promotionRepo;
    @Autowired
    PromotionDtoMapper promotionDtoMapper;
    @Transactional
    public PromotionCreateDto createNewPromotion(PromotionCreateDto promotionCreateDto) {
        if (promotionCreateDto.getPromotionId()!=null) {
            throw new PromotionException("Promotion Id MUST NULL", PromotionErrorCode.BAD_REQUEST);
        }
        validatePromotion(promotionCreateDto);
        promotionCreateDto.setUsedCount(0);
        Promotion promotion = promotionRepo.createNewPromotion(promotionDtoMapper.fromDto(promotionCreateDto));
        //todo: update items and customers
        if (promotionCreateDto.getPromotionItems()!=null) {
        List<Integer> items = promotionCreateDto.getPromotionItems();
        promotionRepo.saveItems(items,promotion);
        }
        if (promotionCreateDto.getPromotionCustomers()!=null) {
        List<Integer> customers = promotionCreateDto.getPromotionCustomers();
        promotionRepo.saveCustomers(customers,promotion);
        }
        return promotionDtoMapper.toDto(promotion);
    }

    public List<PromotionCreateDto> getAllAvailablePromotion(String promotionType) {
        if (promotionType==null) {
            throw new PromotionException("Promotion Type MUST NOT NULL", PromotionErrorCode.BAD_REQUEST);
        }
        if (!(promotionType.equals("PERCENT") || promotionType.equals("BUY_X_GET_Y"))) {
            throw new PromotionException("Promotion Type MUST BE 'PERCENT' or 'BUY_X_GET_Y'", PromotionErrorCode.BAD_REQUEST);
        }
        List<Promotion> promotions = promotionRepo.getAllAvailablePromotion(promotionType);
        return promotions.stream().map(promotionDtoMapper::toDto).toList();
    }

    public List<PromotionCreateDto> getAllPromotion() {
        List<Promotion> promotions = promotionRepo.getAllPromotion();
        return promotions.stream().map(promotionDtoMapper::toDto).toList();
    }

    public PromotionCreateDto getPromotionByCode(String code) {
        Promotion promotion = promotionRepo.getPromotionByCode(code);
        PromotionCreateDto promotionCreateDto = promotionDtoMapper.toDto(promotion);
        List<PromotionItem> promotionItems = promotionRepo.findPromotionItemById(promotion);
        List<PromotionCustomer> customerIds = promotionRepo.findPromotionCustomerById(promotion);
        promotionCreateDto.setPromotionItems(promotionItems.stream().map(PromotionItem::getCatalogItemId).toList());
        promotionCreateDto.setPromotionCustomers(customerIds.stream().map(PromotionCustomer::getCustomerProfileId).
                toList());
        return promotionCreateDto;
    }
    @Transactional
    public PromotionCreateDto updatePromotion(PromotionCreateDto promotionCreateDto) {
        if (promotionCreateDto.getPromotionId()==null) {
            throw new PromotionException("Promotion Id MUST NOT NULL", PromotionErrorCode.BAD_REQUEST);
        }
        validatePromotion(promotionCreateDto);
        Promotion promotion = promotionRepo.updatePromotion(promotionCreateDto.getPromotionId(), promotionDtoMapper.fromDto(promotionCreateDto));
            promotionRepo.deleteOldItems(promotion);
        //todo: update items and customers
        if (promotionCreateDto.getPromotionItems()!=null) {
            List<Integer> items = promotionCreateDto.getPromotionItems();
            promotionRepo.saveItems(items,promotion);
        }
        if (promotionCreateDto.getPromotionCustomers()!=null) {
            List<Integer> customers = promotionCreateDto.getPromotionCustomers();
            promotionRepo.saveCustomers(customers,promotion);
        }
        return promotionDtoMapper.toDto(promotion);
    }
    private void validatePromotion(PromotionCreateDto promotionCreateDto){
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
        if (type.equals("PERCENT")) {
            Assert.notNull(promotionCreateDto.getDiscountPercent(), "PROMOTION_DISCOUNT_PERCENT REQUIRED");
        }
        if (type.equals("BUY_X_GET_Y")) {
            Assert.notNull(promotionCreateDto.getBuyItemId(), "PROMOTION_BUY_ITEM_ID REQUIRED");
            Assert.notNull(promotionCreateDto.getBuyQuantity(), "PROMOTION_BUY_QUANTITY REQUIRED");
            Assert.notNull(promotionCreateDto.getGetItemId(), "PROMOTION_GET_ITEM_ID REQUIRED");
            Assert.notNull(promotionCreateDto.getGetQuantity(), "PROMOTION_GET_QUANTITY REQUIRED");
        }
        if (targetType.equals("SPECIFIC")) {
            Assert.notNull(promotionCreateDto.getPromotionCustomers(), "PROMOTION_PROMOTION_CUSTOMERS REQUIRED");
            Assert.notEmpty(promotionCreateDto.getPromotionCustomers(), "PROMOTION_PROMOTION_CUSTOMERS REQUIRED");
        }
        if (promotionCreateDto.getApplyTo().equals("SPECIFIC")) {
            Assert.notNull(promotionCreateDto.getPromotionItems(), "PROMOTION_PROMOTION_ITEMS REQUIRED");
            Assert.notEmpty(promotionCreateDto.getPromotionItems(), "PROMOTION_PROMOTION_ITEMS REQUIRED");
        }
    }
}
