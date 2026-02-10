package com.g42.platform.gms.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "catalog_item")
@Data
public class CatalogItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private ItemType itemType; // SERVICE hoặc PART

    @Column(name = "category_id")
    private Integer categoryId; // Foreign key to ItemCategory

    @Column(name = "warranty_duration_months")
    private Integer warrantyDurationMonths = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    public enum ItemType {
        SERVICE,
        PART
    }
}