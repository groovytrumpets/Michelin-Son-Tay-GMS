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
    @NotBlank
    private String unit;
    private Integer comboDurationMonths;
    private String comboDescription;
    private Boolean isRecurring;
    private Integer brandId;
    private Integer productLineId;
    @NotNull
    private Integer itemCategoryId;

}
