package com.g42.platform.gms.warehouse.api.dto.response;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import lombok.Data;

@Data
public class StockAllocationResult {
    private Integer allocationId;
    private Integer serviceTicketId;
    private Integer estimateItemId;
    private Integer warehouseId;
    private Integer itemId;
    private Integer quantity;
    private AllocationStatus status;
}
