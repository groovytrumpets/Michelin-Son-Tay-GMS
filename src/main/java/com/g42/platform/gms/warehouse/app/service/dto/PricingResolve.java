package com.g42.platform.gms.warehouse.app.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PricingResolve {
    private BigDecimal finalPrice;
    private String notify;
}
