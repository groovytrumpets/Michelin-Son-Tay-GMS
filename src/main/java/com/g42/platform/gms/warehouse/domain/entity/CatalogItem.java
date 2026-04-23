package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import lombok.*;

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
    private String madeIn;
    private Integer taxRuleId;
    private Integer workCategoryId;
    private String partNumber;
    private String barcode;
    private String color;

    public Integer getBrandId() {
        if (brandId == null) return 0;
        return brandId;
    }

    public Integer getProductLineId() {
        if (productLineId == null) return 0;
        return productLineId;
    }

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