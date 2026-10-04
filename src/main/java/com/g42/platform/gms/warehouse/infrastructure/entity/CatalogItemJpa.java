package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import jakarta.persistence.*;
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

    @Column(nullable = false, length = 500)
    private String itemName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CatalogItemType itemType;



    private Boolean isActive = true;
    @ColumnDefault("0")
    @Column(name = "warranty_duration_months")
    private Integer warrantyDurationMonths;
    @Column(name = "service_service_id", nullable = true)
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

    @Column(name = "brand_id")
    private Integer brandId;

    @Column(name = "product_line_id")
    private Integer productLineId;
    @Size(max = 100)
    @Column(name = "made_in", length = 100)
    private String madeIn;
    @Column(name = "tax_rule_id")
    private Integer taxRuleId;
    @Column(name = "item_category_id")
    private Integer itemCategoryId;
    @Size(max = 50)
    @Column(name = "part_number", length = 50)
    private String partNumber;
    @Size(max = 50)
    @Column(name = "barcode", length = 50)
    private String barcode;
    @Size(max = 50)
    @Size(max = 220)
    @Column(name = "slug", length = 220)
    private String slug;

    @Column(name = "color", length = 50)
    private String color;

    @Size(max = 500)
    @Column(name = "compatible_cars", length = 500)
    private String compatibleCars;

    @Size(max = 2000)
    @Column(name = "search_key", length = 2000)
    private String searchKey;

    @Lob
    @Column(name = "technical_specs")
    private String technicalSpecs;

    @Lob
    @Column(name = "user_guide")
    private String userGuide;

    /** Bảo hành cho đại lý; warrantyDurationMonths là bảo hành khách lẻ. */
    @Column(name = "dealer_warranty_months")
    private Integer dealerWarrantyMonths;

    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "measurement_type", length = 10, nullable = false)
    private String measurementType = "COUNT";
    @Column(name = "decimal_scale", nullable = false)
    private Integer decimalScale = 0;
    @Column(name = "packaging_unit", length = 50)
    private String packagingUnit;
    @Column(name = "conversion_factor", precision = 14, scale = 3, nullable = false)
    private BigDecimal conversionFactor = BigDecimal.ONE;
    @Column(name = "sell_by_package_only", nullable = false)
    private Boolean sellByPackageOnly = false;
    @Column(name = "tracks_lot", nullable = false)
    private Boolean tracksLot = true;
    @Column(name = "tracks_serial", nullable = false)
    private Boolean tracksSerial = false;

    /** Các cột cấu hình đo lường là NOT NULL; mapper copy null từ domain thì trả về mặc định. */
    @PrePersist
    @PreUpdate
    void applyMeasurementDefaults() {
        if (measurementType == null || measurementType.isBlank()) measurementType = "COUNT";
        if (decimalScale == null) decimalScale = 0;
        if (conversionFactor == null || conversionFactor.signum() <= 0) conversionFactor = BigDecimal.ONE;
        if (sellByPackageOnly == null) sellByPackageOnly = false;
        if (tracksLot == null) tracksLot = true;
        if (tracksSerial == null) tracksSerial = false;
    }

}