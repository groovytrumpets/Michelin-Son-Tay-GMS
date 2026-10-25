package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Huỷ phiếu nhập kho — chỉ khi DRAFT */
@Data
public class CancelStockEntryRequest {
    @Size(max = 500, message = "Lý do huỷ tối đa 500 ký tự")
    private String reason;
}
