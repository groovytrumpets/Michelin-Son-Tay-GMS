package com.g42.platform.gms.warehouse.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class LotUpdateDto {
    private Integer entryItemId;
    private Integer remainingQuantity;
    private BigDecimal sellingPrice;
    private BigDecimal sellingPriceWholesale;
    private BigDecimal importPrice;
    private BigDecimal markupMultiplier;
    private BigDecimal markupMultiplierWholesale;

    /**
     * Chi dung cho LO MOI (entryItemId = null): ma lo va ngay nhap do nguoi dung
     * go o popup "Chinh sua danh muc & Ton kho". Bo trong thi backend tu sinh ma.
     */
    private String entryCode;
    private String entryDate;
}
