package com.g42.platform.gms.warehouse.api.dto.request;

import lombok.Data;

@Data
public class CancelStockAllocationRequest {
    private Integer estimateItemId;  // Hủy allocation RESERVED chưa gắn issue
    private Integer issueId;         // Hủy allocation RESERVED đã gắn issue (phiếu xuất chưa confirm)
}