package com.g42.platform.gms.estimation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateItemDto {
    private Integer estimateItemId;
    private String itemName;
    private WorkCataDto workCategory;
    private Integer warehouseId;
    private Integer itemId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
    private BigDecimal taxAmount;
    private BigDecimal appliedTaxRate;
    private String taxCode;
    private Boolean isChecked;
    private Boolean isRemoved;
    private BigDecimal unitPriceWithVat;
    private BigDecimal subTotalWithVat;

}
