package com.g42.platform.gms.warehouse.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Kết quả tổng hợp doanh thu/chi phí/lãi gộp theo item, được tính từ các
 * {@code stock_issue_item} thuộc các phiếu xuất kho CONFIRMED trong một
 * khoảng thời gian (và tùy chọn theo warehouse).
 *
 * Dùng cho report "profit by item" (Treemap: size = grossProfit, color = margin;
 * top purchased items: sort theo totalQuantity).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemProfitAggregate {
    private Integer itemId;
    private BigDecimal totalQuantity;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalGrossProfit;
}
