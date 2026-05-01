package com.g42.platform.gms.warehouse.api.dto.request;

import lombok.Data;

@Data
public class CancelStockAllocationRequest {
    private Integer estimateItemId;  // Hủy allocation RESERVED chưa gắn issue
    private Integer issueId;         // Hủy tất cả allocation RESERVED của phiếu xuất (chưa confirm)
    private Integer issueItemId;     // Hủy allocation của 1 item cụ thể trong phiếu xuất (chưa confirm)
}