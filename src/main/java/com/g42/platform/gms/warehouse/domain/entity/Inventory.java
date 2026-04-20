package com.g42.platform.gms.warehouse.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Domain entity cho tồn kho — thuần POJO, không phụ thuộc JPA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    private Integer inventoryId;
    private Integer warehouseId;
    private Integer itemId;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private LocalDateTime lastUpdated;

    /** Tính số lượng khả dụng (không âm) */
    public int getAvailableQuantity() {
        int qty = quantity != null ? quantity : 0;
        int reserved = reservedQuantity != null ? reservedQuantity : 0;
        return Math.max(0, qty - reserved);
    }
}
