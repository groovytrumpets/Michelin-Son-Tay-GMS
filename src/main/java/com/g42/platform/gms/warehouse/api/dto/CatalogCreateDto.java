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
    private Integer workCategoryId;
    private String partNumber;
    private String barcode;
    private String color;
    private String compatibleCars;
    private Boolean isActive;
    private String origin;
    private String technicalSpecs;
    private String userGuide;
    /** Bảo hành cho đại lý; warrantyDurationMonths là bảo hành khách lẻ. */
    private Integer dealerWarrantyMonths;
    private BigDecimal costPrice;
    /** Danh sách xe tương thích; gửi lên là thay thế toàn bộ danh sách cũ. */
    private java.util.List<CatalogItemCompatDto> compatibilities;
    private java.util.List<WarehouseUpdateDto> warehouseDetails;

}
