package com.g42.platform.gms.estimation.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockAllocation {
    private Integer allocationId;
    private Integer serviceTicketId;
    private Integer estimateItemId;
    private Integer issueId;
    private Integer warehouseId;
    private Integer itemId;
    private Integer quantity;
    private String status;
    private Integer createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer estimateId;
}