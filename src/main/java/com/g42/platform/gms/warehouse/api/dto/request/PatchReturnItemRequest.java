package com.g42.platform.gms.warehouse.api.dto.request;


import java.math.BigDecimal;
import lombok.Data;

/** Sửa từng dòng item trong phiếu hoàn hàng */
@Data
public class PatchReturnItemRequest {
    private BigDecimal quantity;
    private String conditionNote;
}
