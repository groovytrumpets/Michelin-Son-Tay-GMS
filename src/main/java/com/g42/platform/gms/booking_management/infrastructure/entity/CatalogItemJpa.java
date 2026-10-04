package com.g42.platform.gms.booking_management.infrastructure.entity;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
@Getter
@Setter
@Entity
@Table(name = "catalog_item")

public class CatalogItemJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer itemId;

    @Column(nullable = false, length = 500)
    private String itemName;

    @Column(nullable = false)
    private String itemType; // SERVICE hoặc PART

    private Boolean isActive = true;
    @ColumnDefault("0")
    @Column(name = "warranty_duration_months")
    private Integer warrantyDurationMonths;
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_service_id", nullable = false)
    private ServiceJpaEntity serviceService;
    @Column(name = "item_category_id")
    private Integer itemCategoryId;

    // Các cột đọc-thêm phục vụ lọc sản phẩm public (/home/products);
    // ghi/cập nhật các cột này do module warehouse (CatalogItemJpa bên warehouse) đảm nhiệm.
    @Column(name = "price", insertable = false, updatable = false)
    private java.math.BigDecimal price;

    @Column(name = "brand_id", insertable = false, updatable = false)
    private Integer brandId;

    @Column(name = "product_line_id", insertable = false, updatable = false)
    private Integer productLineId;

    @Column(name = "compatible_cars", insertable = false, updatable = false)
    private String compatibleCars;
}