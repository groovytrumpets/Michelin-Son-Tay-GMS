package com.g42.platform.gms.marketing.service_combo.infrastructure.entity;

import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "combo_items")
@Data
public class ComboItemJpa {
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

    @Column(name = "included_item_id", insertable = false, updatable = false)
    private Integer includedItem;
}
