package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.StockTransferStatus;
import com.g42.platform.gms.warehouse.domain.enums.StockTransferType;
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
public class StockTransfer {

    private Integer transferId;
    private String transferCode;
    private Integer fromWarehouseId;
    private Integer toWarehouseId;
    private StockTransferType transferType;
    private StockTransferStatus status;
    private Integer requestedById;
    private Integer approvedById;
    private String notes;
    private Instant createdAt;
    private Instant approvedAt;
    private Instant completedAt;


}