package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategoryReqDto {
    private Integer itemCategoryId;
    private String categoryCode;
    private String categoryName;
    private String categoryType;
    private Byte isActive;
    private Integer taxRuleId;
}
