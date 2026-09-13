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
    private Integer itemCategoryId;
    private String partNumber;
    private String barcode;
    private String color;
    private String slug;
    private String compatibleCars;
    private String searchKey;
    private String technicalSpecs;
    private String userGuide;
    /** Bảo hành cho đại lý; warrantyDurationMonths là bảo hành khách lẻ. */
    private Integer dealerWarrantyMonths;
    private BigDecimal costPrice;
    /** COUNT = đếm (số nguyên), MEASURE = đo lường (cho phép số lẻ). */
    private String measurementType;
    /** Số chữ số thập phân cho phép khi nhập số lượng (0-3). */
    private Integer decimalScale;
    /** Đơn vị nhập lớn (can, phuy, hộp); null = nhập theo đơn vị tồn. */
    private String packagingUnit;
    /** Số đơn vị tồn trong một đơn vị nhập. */
    private BigDecimal conversionFactor;
    /** Chỉ bán theo bội số của conversionFactor. */
    private Boolean sellByPackageOnly;
    /** Cho chọn lô khi bán; false = tự lấy FIFO. */
    private Boolean tracksLot;
    /** Mỗi đơn vị hàng có số serial riêng. */
    private Boolean tracksSerial;

    public Integer getBrandId() {
        return brandId;
    }

    public Integer getProductLineId() {
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