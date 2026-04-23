package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StockAllocation {
    private Integer allocationId;
    private Integer serviceTicketId;
    private Integer issueId;
    private Integer estimateItemId;
    private Integer warehouseId;
    private Integer itemId;
    private String itemName;
    private Integer quantity;
    private AllocationStatus status;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
