package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItem {

    private Integer itemId;
    private String itemName;
    private CatalogItemType itemType;
    private Boolean isActive = true;
    private Integer warrantyDurationMonths;
    private Long serviceServiceId;
    private String sku;
    private BigDecimal price;
    private Boolean showPrice;
    private String description;
    private String imageUrl;
    private String unit;
    private Integer comboDurationMonths;
    private String comboDescription;
    private Boolean isRecurring;
    private Integer brandId;
    private Integer productLineId;
    private Integer itemCategoryId;

    public void validateBrandConsistency(Brand brand, ProductLine productLine) {
        if (!productLine.getBrandId().equals(brand.getBrandId())) {
            throw new WarehouseException("Product line không thuộc brand", WarehouseErrorCode.INVALID_PRODUCT_LINE);
        }
    }
    public void validateService() {
        if (serviceServiceId == null&&itemType==CatalogItemType.SERVICE) {
        throw new WarehouseException("Phải tạo cả service, không tìm thấy service!", WarehouseErrorCode.SERVICE_NOT_FOUND);
        }
    }
}