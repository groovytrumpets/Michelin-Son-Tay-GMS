package com.g42.platform.gms.warehouse.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkCategory {

    private Integer workCategoryId;
    private String categoryCode;
    private String categoryName;
    private Integer displayOrder;
    private Boolean isActive;
    private Integer estimateItemEstimateItem;
    private Boolean isDefault;
    private Integer taxRuleId;
    private String categoryType;
}
