package com.g42.platform.gms.warehouse.api.dto;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CatalogWarehouseDto {
    private Integer itemId;
    private String itemName;
    private CatalogItemType itemType;
    private Integer warrantyDurationMonths;
    private Long serviceServiceId;
    private String sku;
    private BigDecimal price;
    private Boolean showPrice;
    private String description;
    private String imageUrl;
    private String unit;
    private Boolean isRecurring;
    private String brand;
    private String productLine;
    private String itemCategoryCode;
    private String madeIn;
    private Integer taxRuleId;
    private String partNumber;
    private String barcode;
    private List<WarehouseDetailDto> warehouseDetails;
}
