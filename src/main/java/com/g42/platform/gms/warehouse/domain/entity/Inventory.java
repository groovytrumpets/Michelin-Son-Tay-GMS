package com.g42.platform.gms.warehouse.domain.entity;


import java.math.BigDecimal;
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
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private LocalDateTime lastUpdated;

    /** Tính số lượng khả dụng (không âm) */
    public BigDecimal getAvailableQuantity() {
        return com.g42.platform.gms.common.util.Qty.subFloorZero(quantity, reservedQuantity);
    }
}
