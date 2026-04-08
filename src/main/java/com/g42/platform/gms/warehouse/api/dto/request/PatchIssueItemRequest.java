package com.g42.platform.gms.warehouse.api.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/** Sửa từng dòng item trong phiếu xuất kho */
@Data
public class PatchIssueItemRequest {
    private Integer quantity;
    private BigDecimal discountRate;
}
