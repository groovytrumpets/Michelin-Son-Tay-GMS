package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

@Entity(name = "WarehouseCatalogItem")
@Table(name = "catalog_item")
@Data
public class CatalogItemJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;

    @Column(nullable = false)
    private String itemName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalogItemType itemType;



    private Boolean isActive = true;
    @ColumnDefault("0")
    @Column(name = "warranty_duration_months")
    private Integer warrantyDurationMonths;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_service_id", nullable = false)
    private ServiceJpaEntity serviceService;
    @Size(max = 45)
    @NotNull
    @Column(name = "sku", nullable = false, length = 45)
    private String sku;
    @Column(name = "price")
    private Double price;
    @Column(name = "show_price")
    private Boolean showPrice;
    @Lob
    @Column(name = "description")
    private String description;
    @Size(max = 100)
    @Column(name = "image_url", length = 100)
    private String imageUrl;
    @Column(name = "combo_duration_months")
    private Integer comboDurationMonths;
    @Lob
    @Column(name = "combo_description")
    private String comboDescription;
    @ColumnDefault("0")
    @Column(name = "is_recurring")
    private Boolean isRecurring;
}