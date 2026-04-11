package com.g42.platform.gms.warehouse.api.dto.response;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PartResponse {
    private Integer itemId;
    private String itemName;
    private CatalogItemType itemType;
    private String sku;
    private String partNumber;
    private String barcode;
    private String unit;
    private String madeIn;
    private Integer workCategoryId;
    private Integer brandId;
    private Integer productLineId;
    private Boolean isActive;
    private String color;
}
