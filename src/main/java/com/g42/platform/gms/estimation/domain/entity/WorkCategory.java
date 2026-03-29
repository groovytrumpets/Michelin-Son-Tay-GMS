package com.g42.platform.gms.estimation.domain.entity;

import com.g42.platform.gms.estimation.infrastructure.entity.TaxRuleJpa;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkCategory {

    private Integer id;
    private String categoryCode;
    private String categoryName;
    private Integer displayOrder;
    private Boolean isActive;
    private Integer estimateItemEstimateItem;
    private Boolean isDefault;
    private Integer taxRuleId;
}
