package com.g42.platform.gms.warehouse.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Inventory {

    private Integer inventoryId;
    private Integer warehouseId;
    private Integer itemId;
    private Integer quantity;
    private Integer reservedQuantity;
    private BigDecimal importPrice;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private LocalDateTime lastUpdated;

    public int getAvailableQuantity() {
        int qty = quantity != null ? quantity : 0;
        int reserved = reservedQuantity != null ? reservedQuantity : 0;
        return Math.max(0, qty - reserved);
    }
}