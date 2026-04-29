package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class InventoryTransaction {

    private Integer transactionId;
    private Integer warehouseId;
    private Integer itemId;
    private InventoryTransactionType transactionType;
    private Integer quantity;
    private Integer balanceAfter;
    private Integer entryItemId;
    private String referenceType;
    private Integer referenceId;
    private String notes;
    private Integer createdById;
    private Instant createdAt;


}