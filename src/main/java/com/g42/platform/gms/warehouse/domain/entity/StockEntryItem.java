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
public class StockEntryItem {
    private Integer entryItemId;
    private Integer entryId;
    private Integer itemId;
    private Integer quantity;
    private BigDecimal importPrice;
    private BigDecimal markupMultiplier;
    private Integer remainingQuantity;
    private String notes;
}
