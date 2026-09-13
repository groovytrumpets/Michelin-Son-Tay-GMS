package com.g42.platform.gms.warehouse.api.dto;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogCreateDto {

    private String itemName;
    private CatalogItemType itemType;
    private Integer warrantyDurationMonths;
    private Long serviceServiceId;
    @NotBlank
    private String sku;
    @NotNull
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
    /** Đường dẫn chữ tuỳ chỉnh /parts|/services/{slug}; để trống thì giữ nguyên link số. */
    private String slug;
    private String compatibleCars;
    private Boolean isActive;
    private String origin;
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
    /** Danh sách xe tương thích; gửi lên là thay thế toàn bộ danh sách cũ. */
    private java.util.List<CatalogItemCompatDto> compatibilities;
    private java.util.List<WarehouseUpdateDto> warehouseDetails;

}
