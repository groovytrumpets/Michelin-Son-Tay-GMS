package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaxRuleDto {
    private Integer taxRuleId;
    private String taxCode;
    private String taxName;
    private BigDecimal taxRate;
    private String itemType;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Byte isActive;
}
