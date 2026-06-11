package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseLotDto {
    private Integer entryItemId;
    private Integer entryId;
    private String entryCode;
    private Integer quantity;
    private Integer remainingQuantity;
    private BigDecimal importPrice;
    private BigDecimal markupMultiplier;
    private BigDecimal sellingPrice;
}
