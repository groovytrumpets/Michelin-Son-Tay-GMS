package com.g42.platform.gms.warehouse.api.dto.entry;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StockEntryItemRequest {

    @NotNull
    private Integer itemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    /** Giá nhập từ NCC */
    @NotNull
    @Min(0)
    private BigDecimal importPrice;

    /**
     * Hệ số markup fallback — dùng khi warehouse_pricing chưa được cấu hình.
     * Giá bán fallback = importPrice × markupMultiplier.
     * Mặc định 1.0 (bán bằng giá nhập).
     */
    private BigDecimal markupMultiplier = BigDecimal.ONE;

    private String notes;
}
