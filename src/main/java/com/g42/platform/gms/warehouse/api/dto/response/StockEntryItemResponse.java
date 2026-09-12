package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockEntryItemResponse {
    private Integer entryItemId;
    private Integer itemId;
    private String itemName;
    private String sku;
    private Integer quantity;
    private BigDecimal importPrice;
    private BigDecimal markupMultiplier;
    private BigDecimal markupMultiplierWholesale;
    private Integer remainingQuantity;
    private String notes;
}
