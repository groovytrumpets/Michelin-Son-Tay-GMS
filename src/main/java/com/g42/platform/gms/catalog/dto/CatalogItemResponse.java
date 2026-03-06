package com.g42.platform.gms.catalog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogItemResponse {
    private Integer itemId;
    private String itemName;
    private String itemType;
    private Double estimatedPrice;
    private Integer estimateTime;
    private Boolean isActive;
}
