package com.g42.platform.gms.warehouse.api.dto.request;

import lombok.Data;

/** Sửa từng dòng item trong phiếu hoàn hàng */
@Data
public class PatchReturnItemRequest {
    private Integer quantity;
    private String conditionNote;
}
