package com.g42.platform.gms.warehouse.app.service.discount;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.repository.DiscountConfigRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.DiscountConfigJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountConfigRepo discountConfigRepo;
    private final StockEntryRepo stockEntryRepo;
    @Transactional(readOnly = true)
    public BigDecimal calculateFinalPrice(Integer itemId, IssueType issueType,
                                          int quantity, BigDecimal exportPrice) {
        BigDecimal bestRate = discountConfigRepo.findActiveByItemIdAndIssueType(itemId, issueType)
                .stream()
                .filter(c -> c.getQuantityThreshold() == null || quantity >= c.getQuantityThreshold())
                .max(Comparator.comparing(DiscountConfigJpa::getDiscountRate))
                .map(DiscountConfigJpa::getDiscountRate)
                .orElse(BigDecimal.ZERO);

        if (bestRate.compareTo(BigDecimal.ZERO) == 0) return exportPrice;

        return exportPrice
                .multiply(BigDecimal.ONE.subtract(bestRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }
    @Transactional(readOnly = true)
    public boolean isBelowImportPrice(Integer warehouseId, Integer itemId, BigDecimal finalPrice) {
        return stockEntryRepo.findFifoLots(warehouseId, itemId).stream()
                .findFirst()
                .map(lot -> finalPrice.compareTo(lot.getImportPrice()) < 0)
                .orElse(false);
    }
    @Transactional
    public DiscountConfigJpa create(Integer itemId, IssueType issueType,
                                     Integer quantityThreshold, BigDecimal discountRate,
                                     Integer createdBy) {
        DiscountConfigJpa config = new DiscountConfigJpa();
        config.setItemId(itemId);
        config.setIssueType(issueType);
        config.setQuantityThreshold(quantityThreshold);
        config.setDiscountRate(discountRate);
        config.setIsActive(true);
        config.setCreatedBy(createdBy);
        return discountConfigRepo.save(config);
    }
}
