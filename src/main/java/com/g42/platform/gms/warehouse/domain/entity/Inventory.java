package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

    private Integer inventoryId;
    private Integer warehouseId;
    private CatalogItemJpaEntity item;
    private Integer quantity;
    private Integer minStockLevel;
    private Integer maxStockLevel;
    private Instant lastUpdated;


}