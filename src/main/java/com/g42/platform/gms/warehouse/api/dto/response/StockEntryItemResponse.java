package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockEntryItemResponse {
    private Integer entryItemId;
    private Integer itemId;
    private String itemName;
    private String sku;
    private BigDecimal quantity;
    private BigDecimal importPrice;
    private BigDecimal markupMultiplier;
    private BigDecimal markupMultiplierWholesale;
    private BigDecimal remainingQuantity;
    private String notes;
    private String unit;
    private String measurementType;
    private Integer decimalScale;
    private Boolean tracksSerial;
    private String inputUnit;
    private BigDecimal inputQuantity;
    private BigDecimal conversionFactor;
    private java.util.List<String> serialCodes;
}
