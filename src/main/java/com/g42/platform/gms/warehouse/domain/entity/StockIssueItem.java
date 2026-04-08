package com.g42.platform.gms.warehouse.domain.entity;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockIssueItem {
    private Integer issueItemId;
    private Integer issueId;
    private Integer itemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal exportPrice;
    private BigDecimal importPrice;
    private BigDecimal discountRate;
    private BigDecimal finalPrice;
    private BigDecimal grossProfit; // (finalPrice - importPrice) * quantity
}
