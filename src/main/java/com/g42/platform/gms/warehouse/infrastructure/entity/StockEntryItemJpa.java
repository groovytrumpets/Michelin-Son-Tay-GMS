package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Chi tiết từng phụ tùng trong phiếu nhập kho.
 * 1 stock_entry có nhiều stock_entry_item.
 */
@Entity
@Table(name = "stock_entry_item")
@Data
public class StockEntryItemJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entry_item_id")
    private Integer entryItemId;

    @Column(name = "entry_id", nullable = false)
    private Integer entryId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "import_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal importPrice;

    /**
     * Hệ số markup fallback — dùng khi warehouse_pricing chưa được cấu hình.
     * selling_price_fallback = import_price × markup_multiplier
     */
    @Column(name = "markup_multiplier", nullable = false, precision = 6, scale = 4)
    private BigDecimal markupMultiplier = BigDecimal.ONE;

    /** Số lượng còn lại trong lô này — giảm dần theo FIFO khi xuất */
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
