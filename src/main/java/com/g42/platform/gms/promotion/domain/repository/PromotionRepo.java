package com.g42.platform.gms.promotion.domain.repository;

import com.g42.platform.gms.billing.api.dto.ServiceBillDto;
import com.g42.platform.gms.promotion.domain.entity.Promotion;
import com.g42.platform.gms.promotion.domain.entity.PromotionCustomer;
import com.g42.platform.gms.promotion.domain.entity.PromotionItem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRepo {
    Promotion createNewPromotion(Promotion promotionCreateDto);

    List<Promotion> getAllPromotion();

    Promotion getAllPromotionForBilling(ServiceBillDto serviceBillDto);

    List<Promotion> getAllAvailablePromotion(String promotionType);

    Promotion getPromotionByCode(String code);

    Promotion updatePromotion(Integer promotionId, Promotion promotion);

    void countUsed(Integer promotionId);

    void saveItems(List<Integer> items, Promotion promotionId);

    void saveCustomers(List<Integer> customers, Promotion promotion);

    List<PromotionItem> findPromotionItemById(Promotion promotionId);

    List<PromotionCustomer> findPromotionCustomerById(Promotion promotion);

    void deleteOldItems(Promotion promotion);
}
