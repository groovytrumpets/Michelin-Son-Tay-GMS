package com.g42.platform.gms.marketing.service_catalog.api.dto;

import com.g42.platform.gms.marketing.service_catalog.domain.enums.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSumaryRespond {
    private int catalogItemId;
    private Long serviceId;
    private String title;
    private String shortDescription;
    private boolean showPrice;
    private String displayPrice;
    private String mediaThumbnail;
    private ServiceStatus status;

    // Thông tin bổ sung từ kho cho trang public (lọc danh mục/hãng/dòng xe + tồn kho)
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
    private Boolean inStock;
    private java.math.BigDecimal availableQty;

    /** Slug bài viết item_post PUBLISHED gắn với catalogItemId — null nếu chưa có bài viết (FE fallback về id số). */
    private String slug;
}
