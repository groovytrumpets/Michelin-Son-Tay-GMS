package com.g42.platform.gms.estimation.api.dto.taxRule;

import java.math.BigDecimal;
import java.time.LocalDate;

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
