package com.g42.platform.gms.estimation.infrastructure.entity;

import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.promotion.infrastructure.entity.PromotionJpa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "estimate_item", schema = "michelin_garage")
public class EstimateItemJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estimate_item_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "estimate_id", nullable = false)
    private Integer estimateId;

    @Size(max = 255)
    @Column(name = "item_name")
    private String itemName;


    @Column(name = "item_id")
    private Integer itemId;


    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @ColumnDefault("0")
    @Column(name = "is_overridden")
    private Boolean isOverridden;

    @Size(max = 255)
    @Column(name = "override_reason")
    private String overrideReason;

    /** THÀNH TIỀN gõ tay của dòng; NULL nghĩa là tính tự động theo SL x đơn giá x thuế. */
    @Column(name = "manual_line_total", precision = 12, scale = 2)
    private BigDecimal manualLineTotal;

    @Column(name = "warehouse_id")
    private Integer warehouseId;
    /** Danh mục là tùy chọn — dòng báo giá không bắt buộc thuộc nhóm nào. */
    @Column(name = "item_category_id")
    private Integer itemCategoryId;

    /** Tên nhóm gõ tay của riêng dòng này, không tạo bản ghi danh mục dùng chung. */
    @Size(max = 255)
    @Column(name = "category_label", length = 255)
    private String categoryLabel;

    @Column(name = "total_price", precision = 12, scale = 2)
    private BigDecimal totalPrice;
    @ColumnDefault("0")
    @Column(name = "is_checked")
    private Boolean isChecked;
    @ColumnDefault("0")
    @Column(name = "is_removed")
    private Boolean isRemoved;
    @Column(name = "tax_amount", precision = 12, scale = 2)
    private BigDecimal taxAmount;
    @Column(name = "applied_tax_rate", precision = 5, scale = 2)
    private BigDecimal appliedTaxRate;
    @Size(max = 50)
    @Column(name = "unit", length = 50)
    private String unit;
    @Column(name = "revised_from_item_id")
    private Integer revisedFromItemId;
    @Column(name = "promotion_id")
    private Integer promotionId;
    @ColumnDefault("0")
    @Column(name = "is_gift")
    private Boolean isGift;
    @Column(name = "triggered_by_item_id")
    private Integer triggeredByItemId;
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "final_price", precision = 12, scale = 2)
    private BigDecimal finalPrice;
    @Column(name = "gross_profit", precision = 12, scale = 2)
    private BigDecimal grossProfit;
    @Column(name = "entry_item_id")
    private Integer entryItemId;

    @ColumnDefault("0")
    @Column(name = "is_outsource")
    private Boolean isOutsource;

    @Column(name = "outsource_partner_id")
    private Integer outsourcePartnerId;

    @Size(max = 500)
    @Column(name = "outsource_work_content", length = 500)
    private String outsourceWorkContent;

    @Column(name = "labor_cost", precision = 12, scale = 2)
    private BigDecimal laborCost;

    @Size(max = 500)
    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "discount_percent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    /** NORMAL | TRADE_IN (gara thu mua linh kiện của khách, thành tiền âm). */
    @ColumnDefault("'NORMAL'")
    @Column(name = "line_type", length = 20, nullable = false)
    private String lineType = "NORMAL";

    /** Số tiền giảm giá nhập tay (không qua mã); gõ % thì là số tiền tính ra từ discount_percent. */
    @Column(name = "manual_discount_amount", precision = 12, scale = 2)
    private BigDecimal manualDiscountAmount;

    /** MapStruct chép cả null từ domain sang, nên chốt lại giá trị mặc định ngay trước khi ghi. */
    @PrePersist
    @PreUpdate
    void defaultLineType() {
        if (lineType == null || lineType.isBlank()) lineType = "NORMAL";
    }

    /** Mảng JSON serial_id đã chọn cho dòng (hàng theo dõi serial). */
    @Column(name = "serial_ids", length = 2000)
    private String serialIdsJson;
}