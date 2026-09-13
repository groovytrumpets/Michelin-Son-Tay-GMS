package com.g42.platform.gms.warehouse.api.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO đại diện cho 1 lô hàng (lot) trong kho.
 * Được dùng trong query JPQL tại StockEntryItemJpaRepo.findWarehouseLots(...)
 */
@Data
public class WarehouseLotDto {

    private Integer entryItemId;
    private Integer entryId;
    private String entryCode;
    private BigDecimal quantity;
    private BigDecimal remainingQuantity;
    private BigDecimal importPrice;
    private BigDecimal markupMultiplier;
    private BigDecimal markupMultiplierWholesale;
    private java.time.LocalDate entryDate;
    /** Hạn dùng của lô; null nghĩa là không theo dõi hạn. Dùng cho chiến lược FEFO. */
    private java.time.LocalDate expiryDate;
    /** Giá bán — được tính và set sau khi query, không lấy từ DB trực tiếp */
    private BigDecimal sellingPrice;
    private BigDecimal sellingPriceWholesale;
    /** Số serial còn trong kho (IN_STOCK + RESERVED) của lô — chỉ có ý nghĩa với hàng theo serial. */
    private Long serialCount;

    public WarehouseLotDto(
            Integer entryItemId,
            Integer entryId,
            String entryCode,
            BigDecimal quantity,
            BigDecimal remainingQuantity,
            BigDecimal importPrice,
            BigDecimal markupMultiplier,
            BigDecimal markupMultiplierWholesale,
            java.time.LocalDate entryDate,
            java.time.LocalDate expiryDate,
            BigDecimal sellingPrice,
            BigDecimal sellingPriceWholesale) {
        this.expiryDate        = expiryDate;
        this.entryItemId       = entryItemId;
        this.entryId           = entryId;
        this.entryCode         = entryCode;
        this.quantity          = quantity;
        this.remainingQuantity = remainingQuantity;
        this.importPrice       = importPrice;
        this.markupMultiplier  = markupMultiplier;
        this.markupMultiplierWholesale = markupMultiplierWholesale;
        this.entryDate         = entryDate;
        this.sellingPrice      = sellingPrice;
        this.sellingPriceWholesale = sellingPriceWholesale;
    }
}
