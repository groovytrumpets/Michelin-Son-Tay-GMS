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
    private Integer availableStockLevel;
    private String notify;
    private java.util.List<WarehouseLotDto> lots;

    public Integer getAvailableStockLevel() {
        this.setAvailableStockLevel(this.quantity-this.reservedQuantity);
        return availableStockLevel;
    }

    public WarehouseDetailDto(Integer warehouseId, String warehouseCode, String warehouseName, String warehouseAddress, Integer itemId, Integer quantity, Integer reservedQuantity, Integer minStockLevel, Integer maxStockLevel) {
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.warehouseAddress = warehouseAddress;
        this.itemId = itemId;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.minStockLevel = minStockLevel;
        this.maxStockLevel = maxStockLevel;
    }

    public WarehouseDetailDto(Integer warehouseId, String warehouseCode, String warehouseName, String warehouseAddress, Integer itemId, BigDecimal sellingPrice, Integer quantity, Integer reservedQuantity, Integer minStockLevel, Integer maxStockLevel, Integer availableStockLevel, String notify) {
        this.warehouseId = warehouseId;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.warehouseAddress = warehouseAddress;
        this.itemId = itemId;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.minStockLevel = minStockLevel;
        this.maxStockLevel = maxStockLevel;
        this.availableStockLevel = availableStockLevel;
        this.notify = notify;
    }
}
