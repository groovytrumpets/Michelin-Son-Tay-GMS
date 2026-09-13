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
    @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = false)
    private BigDecimal quantity;

    /** Giá nhập từ NCC */
    @NotNull
    @Min(0)
    private BigDecimal importPrice;

    /**
     * Hệ số markup fallback — dùng khi warehouse_pricing chưa được cấu hình.
     * Giá bán fallback = importPrice × markupMultiplier.
     * Mặc định 1.0 (bán bằng giá nhập).
     */
    private BigDecimal markupMultiplier;
    private BigDecimal markupMultiplierWholesale;

    private String notes;

    /** Đơn vị nhập (vd "Phuy"); null hoặc trùng đơn vị tồn nghĩa là quantity đã là số theo đơn vị tồn. */
    private String inputUnit;

    /** Số lượng theo inputUnit; backend tự nhân hệ số quy đổi của sản phẩm để ra quantity. */
    private BigDecimal inputQuantity;

    /** Serial nhập kèm (sản phẩm theo dõi serial); được phép ít hơn số lượng, khai báo thêm sau. */
    private java.util.List<String> serialCodes;
}
