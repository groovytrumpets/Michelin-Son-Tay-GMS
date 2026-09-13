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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", insertable = false, updatable = false)
    private CatalogItemJpa catalogItem;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "import_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal importPrice;

    /**
     * Hệ số markup fallback — dùng khi warehouse_pricing chưa được cấu hình.
     * selling_price_fallback = import_price × markup_multiplier
     */
    @Column(name = "markup_multiplier", precision = 6, scale = 4)
    private BigDecimal markupMultiplier;

    @Column(name = "markup_multiplier_wholesale", precision = 6, scale = 4)
    private BigDecimal markupMultiplierWholesale;

    /** Số lượng còn lại trong lô này — giảm dần theo FIFO khi xuất */
    @Column(name = "remaining_quantity", nullable = false)
    private BigDecimal remainingQuantity = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** Hạn dùng của lô; null nghĩa là không theo dõi hạn. Dùng cho chiến lược FEFO. */
    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    /** Đơn vị người dùng nhập (phuy, can); null = nhập theo đơn vị tồn. */
    @Column(name = "input_unit", length = 50)
    private String inputUnit;

    /** Số lượng theo inputUnit; quantity = inputQuantity × conversionFactor. */
    @Column(name = "input_quantity", precision = 14, scale = 3)
    private BigDecimal inputQuantity;

    /** Hệ số quy đổi chụp lại lúc nhập. */
    @Column(name = "conversion_factor", precision = 14, scale = 3)
    private BigDecimal conversionFactor;

    /** Mảng JSON serial nhập kèm khi phiếu còn nháp. */
    @Column(name = "serial_codes", columnDefinition = "TEXT")
    private String serialCodes;
}
