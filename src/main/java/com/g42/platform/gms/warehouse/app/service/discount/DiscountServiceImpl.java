package com.g42.platform.gms.warehouse.app.service.discount;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.DiscountConfigJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.DiscountConfigJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiscountServiceImpl implements DiscountService {

    private final DiscountConfigJpaRepo discountConfigJpaRepo;
    private final StockEntryRepo stockEntryRepo;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateFinalPrice(Integer itemId, IssueType issueType,
                                          int quantity, BigDecimal exportPrice) {
        List<DiscountConfigJpa> configs =
                discountConfigJpaRepo.findActiveByItemIdAndIssueType(itemId, issueType);

        BigDecimal bestRate = configs.stream()
                .filter(c -> c.getQuantityThreshold() == null || quantity >= c.getQuantityThreshold())
                .max(Comparator.comparing(DiscountConfigJpa::getDiscountRate))
                .map(DiscountConfigJpa::getDiscountRate)
                .orElse(BigDecimal.ZERO);

        if (bestRate.compareTo(BigDecimal.ZERO) == 0) {
            return exportPrice;
        }

        return exportPrice
                .multiply(BigDecimal.ONE.subtract(bestRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBelowImportPrice(Integer warehouseId, Integer itemId, BigDecimal finalPrice) {
        // Lấy import_price từ lô FIFO đầu tiên còn hàng
        return stockEntryRepo.findFifoLots(warehouseId, itemId).stream()
                .findFirst()
                .map(lot -> finalPrice.compareTo(lot.getImportPrice()) < 0)
                .orElse(false);
    }
}
