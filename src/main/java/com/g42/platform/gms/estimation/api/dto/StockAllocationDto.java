package com.g42.platform.gms.estimation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAllocationDto {
//    private Integer allocationId;
    private Integer serviceTicketId;
    private Integer estimateItemId;
    private Integer warehouseId;
    private Integer itemId;
    private Integer estimateId;
    private Integer quantity;
    private String status;
    private Integer createdBy;
}
