package com.g42.platform.gms.warehouse.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockIssueItem {
    private Integer issueItemId;
    private Integer issueId;
    private Integer itemId;
    private Integer entryItemId;
    private Integer quantity;
    private BigDecimal exportPrice;
    private BigDecimal estimateUnitPrice;
    private BigDecimal importPrice;
    private BigDecimal discountRate;
    private BigDecimal finalPrice;
    private BigDecimal grossProfit;
}
