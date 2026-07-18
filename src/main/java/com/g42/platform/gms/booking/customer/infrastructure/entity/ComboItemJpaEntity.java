package com.g42.platform.gms.booking.customer.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "combo_items")
@Data
public class ComboItemJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "combo_item_id")
    private Integer comboItemId;

    @Column(name = "combo_id", nullable = false)
    private Integer comboId;

    @Column(name = "included_item_id", nullable = false)
    private Integer includedItemId;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "odometer_km")
    private Integer odometerKm = 0;

    @Column(name = "allocation_method")
    private String allocationMethod = "FIFO";

    @Column(name = "entry_item_id")
    private Integer entryItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "included_item_id", insertable = false, updatable = false)
    private CatalogItemJpaEntity includedItem;
}
