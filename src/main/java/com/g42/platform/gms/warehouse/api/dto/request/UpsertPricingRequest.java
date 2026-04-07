package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpsertPricingRequest {

    @NotNull
    private Integer warehouseId;

    @NotNull
    private Integer itemId;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal basePrice;

    /** Hệ số markup, mặc định 1.0 (không markup) */
    private BigDecimal markupMultiplier = BigDecimal.ONE;

    /** Giá bán trực tiếp — nếu set thì bỏ qua markupMultiplier */
    private BigDecimal sellingPrice;

    private LocalDate effectiveFrom;

    /** null = vô thời hạn */
    private LocalDate effectiveTo;
}
