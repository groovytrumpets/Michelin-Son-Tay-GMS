package com.g42.platform.gms.estimation.infrastructure.entity;

import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "stock_allocation", schema = "michelin_garage")
public class StockAllocationJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id", nullable = false)
    private Integer allocationId;

    @NotNull
    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;

    @NotNull
    @Column(name = "estimate_item_id", nullable = false)
    private Integer estimateItemId;

    @NotNull
    @Column(name = "warehouse_id", nullable = false)
    private Integer warehouseId;

    @NotNull
    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @ColumnDefault("'RESERVED'")
    @Lob
    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


}