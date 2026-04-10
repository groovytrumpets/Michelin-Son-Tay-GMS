package com.g42.platform.gms.catalog.dto;

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

    private Integer estimateTime;
    private Boolean isActive;
}
