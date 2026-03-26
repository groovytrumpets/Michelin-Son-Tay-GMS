package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.estimation.infrastructure.entity.WarehouseJpa;
import com.g42.platform.gms.warehouse.domain.enums.StockTransferStatus;
import com.g42.platform.gms.warehouse.domain.enums.StockTransferType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "stock_transfer", schema = "michelin_garage")
public class StockTransferJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id", nullable = false)
    private Integer transferId;

    @Size(max = 50)
    @NotNull
    @Column(name = "transfer_code", nullable = false, length = 50)
    private String transferCode;

    @NotNull
    @Column(name = "from_warehouse_id", nullable = false)
    private Integer fromWarehouseId;

    @NotNull
    @Column(name = "to_warehouse_id", nullable = false)
    private Integer toWarehouseId;

    @NotNull
    @ColumnDefault("'TRANSFER'")
    @Lob
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false)
    private StockTransferType transferType;

    @NotNull
    @ColumnDefault("'DRAFT'")
    @Lob
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StockTransferStatus status;

    @NotNull
    @Column(name = "requested_by", nullable = false)
    private Integer requestedById;

    @Column(name = "approved_by")
    private Integer approvedById;

    @Lob
    @Column(name = "notes")
    private String notes;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "completed_at")
    private Instant completedAt;


}