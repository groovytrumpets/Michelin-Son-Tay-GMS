package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "inventory", schema = "michelin_garage")
public class InventoryJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id", nullable = false)
    private Integer inventoryId;

    @NotNull
    @Column(name = "warehouse_id", nullable = false)
    private Integer warehouseId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "item_id", nullable = false)
    private CatalogItemJpaEntity item;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @ColumnDefault("0")
    @Column(name = "min_stock_level")
    private Integer minStockLevel;

    @ColumnDefault("0")
    @Column(name = "max_stock_level")
    private Integer maxStockLevel;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "last_updated")
    private Instant lastUpdated;


}