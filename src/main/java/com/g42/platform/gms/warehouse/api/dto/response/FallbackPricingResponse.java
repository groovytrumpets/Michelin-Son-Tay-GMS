package com.g42.platform.gms.warehouse.api.dto.response;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class FallbackPricingResponse {
    private Integer id;
    private String name;
    private CatalogItemType itemType;
    private BigDecimal markupMultiplier;
    private BigDecimal markupMultiplierWholesale;
    private String description;
    private Boolean isActive;
    private Instant createdAt;
}
