package com.g42.platform.gms.warehouse.api.dto;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItemDto {
    private Integer itemId;
    private String itemName;
    private CatalogItemType itemType;
    private Boolean isActive;
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
    private Integer taxRuleId;
    private Integer productLineId;
    private Integer itemCategoryId;
    private String compatibleCars;
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
    private String partNumber;
    private String barcode;
    private String color;
    private String madeIn;
    private String slug;
}
