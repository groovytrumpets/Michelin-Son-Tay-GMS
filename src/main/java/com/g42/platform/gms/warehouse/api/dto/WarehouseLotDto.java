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
    private Integer quantity;
    private Integer remainingQuantity;
    private BigDecimal importPrice;
    private BigDecimal markupMultiplier;
    private java.time.LocalDate entryDate;
    /** Giá bán — được tính và set sau khi query, không lấy từ DB trực tiếp */
    private BigDecimal sellingPrice;

    public WarehouseLotDto(
            Integer entryItemId,
            Integer entryId,
            String entryCode,
            Integer quantity,
            Integer remainingQuantity,
            BigDecimal importPrice,
            BigDecimal markupMultiplier,
            java.time.LocalDate entryDate,
            BigDecimal sellingPrice) {
        this.entryItemId       = entryItemId;
        this.entryId           = entryId;
        this.entryCode         = entryCode;
        this.quantity          = quantity;
        this.remainingQuantity = remainingQuantity;
        this.importPrice       = importPrice;
        this.markupMultiplier  = markupMultiplier;
        this.entryDate         = entryDate;
        this.sellingPrice      = sellingPrice;
    }
}
