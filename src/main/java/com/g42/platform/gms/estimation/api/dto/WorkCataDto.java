package com.g42.platform.gms.estimation.api.dto;

import com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkCataDto {
    private Integer workCateId;
    private String categoryCode;
    private String categoryName;
    private Integer displayOrder;
    private Boolean isDefault;
    private Integer taxRuleId;
}
