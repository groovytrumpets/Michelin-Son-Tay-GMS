package com.g42.platform.gms.estimation.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateItemReqDto {
    private Integer workCategoryId;
    private String newCategoryName;
    private Integer itemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Integer taxRuleId;
    private Boolean isChecked;
}
