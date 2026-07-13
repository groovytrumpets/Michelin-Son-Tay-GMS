package com.g42.platform.gms.warehouse.api.dto.request;

import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpsertFallbackPricingRequest {

    @NotBlank(message = "Tên quy tắc không được để trống")
    private String name;

    private CatalogItemType itemType;

    @NotNull(message = "Hệ số markup lẻ không được để trống")
    @DecimalMin(value = "0.0001", message = "Hệ số markup lẻ phải lớn hơn 0")
    private BigDecimal markupMultiplier;

    @NotNull(message = "Hệ số markup sỉ không được để trống")
    @DecimalMin(value = "0.0001", message = "Hệ số markup sỉ phải lớn hơn 0")
    private BigDecimal markupMultiplierWholesale;

    private String description;
}
