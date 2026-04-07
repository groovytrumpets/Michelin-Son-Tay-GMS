package com.g42.platform.gms.warehouse.app.service.discount;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;

import java.math.BigDecimal;

public interface DiscountService {

    /** Tính giá sau chiết khấu dựa trên issue_type và quantity */
    BigDecimal calculateFinalPrice(Integer itemId, IssueType issueType,
                                   int quantity, BigDecimal exportPrice);

    /** Kiểm tra final_price < import_price (cảnh báo bán lỗ) */
    boolean isBelowImportPrice(Integer warehouseId, Integer itemId, BigDecimal finalPrice);
}
