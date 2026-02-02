package com.g42.platform.gms.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "catalog_item")
@Data
public class CatalogItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private String itemType; // SERVICE hoặc PART

    private Double estimatedPrice;

    private Boolean isActive = true;
}