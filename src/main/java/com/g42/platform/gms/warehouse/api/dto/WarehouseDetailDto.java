package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDetailDto {
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseAddress;
    private Integer itemId;

    private BigDecimal sellingPrice;



    //inventory


    private Integer quantity;
    private Integer reservedQuantity;
    private Integer minStockLevel;
    private Integer maxStockLevel;


    public Integer getAvailableQuantity() {
        if (quantity == null) return 0;
        int reserved = (reservedQuantity != null) ? reservedQuantity : 0;
        return Math.max(0, quantity - reserved);
    }

}
