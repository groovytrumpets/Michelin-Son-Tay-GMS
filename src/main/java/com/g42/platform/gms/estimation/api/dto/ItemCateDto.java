package com.g42.platform.gms.estimation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCateDto {
    private Integer itemCategoryId;
    private String categoryCode;
    private String categoryName;
    private String categoryType;
    private Integer displayOrder;
    private Boolean isActive;
    private Integer taxRuleId;
}
