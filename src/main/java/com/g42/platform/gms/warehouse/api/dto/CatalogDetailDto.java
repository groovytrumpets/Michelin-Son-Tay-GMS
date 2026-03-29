package com.g42.platform.gms.warehouse.api.dto;

import com.g42.platform.gms.warehouse.domain.entity.SpecAttribute;
import com.g42.platform.gms.warehouse.domain.entity.Specification;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogDetailDto {
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
    private String brandId;
    private String productLine;
    private Integer itemCategory;
    private List<SpecificationRespondDto> specifications;
}
