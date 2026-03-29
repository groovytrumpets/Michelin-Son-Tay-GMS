package com.g42.platform.gms.estimation.api.dto.taxRule;

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
public class TaxCreateDto {
    private String taxName;
    private BigDecimal taxRate;
}
