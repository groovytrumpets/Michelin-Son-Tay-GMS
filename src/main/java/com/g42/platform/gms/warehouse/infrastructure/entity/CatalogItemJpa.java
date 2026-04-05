package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

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
    @Column(name = "service_service_id", nullable = false)
    private Long serviceId;

    private String sku;
    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;
    @Column(name = "show_price")
    private Boolean showPrice;
    @Lob
    @Column(name = "description")
    private String description;
    @Size(max = 100)
    @Column(name = "image_url", length = 100)
    private String imageUrl;
    @Size(max = 50)
    @Column(name = "unit", length = 50)
    private String unit;
    @Column(name = "combo_duration_months")
    private Integer comboDurationMonths;
    @Lob
    @Column(name = "combo_description")
    private String comboDescription;
    @ColumnDefault("0")
    @Column(name = "is_recurring")
    private Boolean isRecurring;

    @Column(name = "brand_id", nullable = false)
    private Integer brandId;

    @Column(name = "product_line_id", nullable = false)
    private Integer productLineId;
    @Size(max = 100)
    @Column(name = "made_in", length = 100)
    private String madeIn;
    @Column(name = "tax_rule_id")
    private Integer taxRuleId;
    @NotNull
    @Column(name = "work_category_id", nullable = false)
    private Integer workCategoryId;
    @Size(max = 50)
    @Column(name = "part_number", length = 50)
    private String partNumber;
    @Size(max = 50)
    @Column(name = "barcode", length = 50)
    private String barcode;

}