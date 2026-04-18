package com.g42.platform.gms.warehouse.app.service.discount;

import com.g42.platform.gms.warehouse.domain.entity.DiscountConfig;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.repository.DiscountConfigRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
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
        public BigDecimal resolveDiscountRate(Integer itemId, IssueType issueType, int quantity) {
        return discountConfigRepo.findActiveByItemIdAndIssueType(itemId, issueType)
            .stream()
            .filter(c -> c.getQuantityThreshold() == null || quantity >= c.getQuantityThreshold())
            .max(java.util.Comparator
                .comparingInt(this::specificityScore)
                .thenComparingInt(c -> c.getQuantityThreshold() != null ? c.getQuantityThreshold() : 0)
                .thenComparing(DiscountConfig::getDiscountRate))
            .map(DiscountConfig::getDiscountRate)
            .orElse(BigDecimal.ZERO);
        }

    @Transactional(readOnly = true)
    public BigDecimal calculateFinalPrice(Integer itemId, IssueType issueType,
                                          int quantity, BigDecimal exportPrice) {
        BigDecimal bestRate = resolveDiscountRate(itemId, issueType, quantity);

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
    public DiscountConfig create(Integer itemId, IssueType issueType,
                                     Integer quantityThreshold, BigDecimal discountRate,
                                     Integer createdBy) {
        DiscountConfig config = new DiscountConfig();
        config.setItemId(itemId);
        config.setIssueType(issueType);
        config.setQuantityThreshold(quantityThreshold);
        config.setDiscountRate(discountRate);
        config.setIsActive(true);
        config.setCreatedBy(createdBy);
        return discountConfigRepo.save(config);
    }

    private int specificityScore(DiscountConfig config) {
        int score = 0;
        if (config.getItemId() != null) {
            score += 2;
        }
        if (config.getIssueType() != null) {
            score += 1;
        }
        return score;
    }
}
