package com.g42.platform.gms.warehouse.api.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/** Sửa từng dòng item trong phiếu nhập kho */
@Data
public class PatchEntryItemRequest {
    private Integer quantity;
    private BigDecimal importPrice;
    private BigDecimal markupMultiplier;
    private String notes;
}
