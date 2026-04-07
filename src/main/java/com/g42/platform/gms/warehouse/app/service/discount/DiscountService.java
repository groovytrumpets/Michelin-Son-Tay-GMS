package com.g42.platform.gms.warehouse.app.service.discount;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.infrastructure.entity.DiscountConfigJpa;

import java.math.BigDecimal;

public interface DiscountService {

    /** Tính giá sau discount dựa trên config */
    BigDecimal calculateFinalPrice(Integer itemId, IssueType issueType,
                                   int quantity, BigDecimal exportPrice);

    /** Kiểm tra giá bán có thấp hơn giá vốn không */
    boolean isBelowImportPrice(Integer warehouseId, Integer itemId, BigDecimal finalPrice);

    /** Tạo cấu hình discount mới */
    DiscountConfigJpa create(Integer itemId, IssueType issueType,
                              Integer quantityThreshold, BigDecimal discountRate,
                              Integer createdBy);
}
