package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Thông tin bổ sung của catalog item cho trang public /home/products:
 * hạng mục báo giá (work category), hãng/dòng sản phẩm, xe tương thích và tồn kho khả dụng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomeCatalogItemInfoDto {
    private Integer itemId;
    private String itemType;
    private BigDecimal price;
    private Integer itemCategoryId;
    private String categoryCode;
    private String categoryName;
    private Integer brandId;
    private String brandName;
    private Integer productLineId;
    private String productLineName;
    private String compatibleCars;
    private Integer availableQty;
}
