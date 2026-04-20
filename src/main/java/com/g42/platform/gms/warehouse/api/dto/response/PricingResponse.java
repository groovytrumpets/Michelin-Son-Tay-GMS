package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PricingResponse {
    private Integer pricingId;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Integer itemId;
    private String itemName;
    private BigDecimal basePrice;
    private BigDecimal markupMultiplier;
    private BigDecimal sellingPrice;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
}
