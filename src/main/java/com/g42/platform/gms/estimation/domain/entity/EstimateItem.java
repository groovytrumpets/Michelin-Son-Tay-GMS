package com.g42.platform.gms.estimation.domain.entity;

import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateItem {

    private Integer id;
    private Integer estimateId;
    private String itemName;
    private Integer itemId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private Boolean isOverridden;
    private String overrideReason;
    /** THÀNH TIỀN gõ tay; khi khác null thì dùng thẳng số này, bỏ qua SL x đơn giá x thuế. */
    private BigDecimal manualLineTotal;
    private Integer warehouseId;
    private Integer itemCategoryId;
    /**
     * Tên nhóm advisor gõ tay cho riêng dòng này. Trước đây mỗi lần gõ một cái tên
     * chưa có là hệ thống đẻ thêm một bản ghi danh mục, làm bảng danh mục ngập rác;
     * nay chỉ lưu chữ, không đụng tới danh mục dùng chung.
     */
    private String categoryLabel;
    private BigDecimal totalPrice;
    private Boolean isChecked;
    private Boolean isRemoved = false;
    private BigDecimal taxAmount;
    private BigDecimal appliedTaxRate;
    private String unit;
    private Integer revisedFromItemId;
    private Integer promotionId;
    private Boolean isGift;
    private Integer triggeredByItemId;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private BigDecimal grossProfit;
    private Integer entryItemId;
    private BigDecimal importPrice;
    private Boolean isOutsource;
    private Integer outsourcePartnerId;
    private String outsourceWorkContent;
    private BigDecimal laborCost;
    private String note;
    private BigDecimal discountPercent;
    /** NORMAL | TRADE_IN — xem EstimateLineType. */
    private String lineType;
    /** Số tiền giảm giá nhập tay của dòng (không qua mã). */
    private BigDecimal manualDiscountAmount;
    /** Mảng JSON serial_id đã chọn cho dòng. */
    private String serialIdsJson;

    public BigDecimal getSubTotal() {
        if (unitPrice == null || quantity == null) return BigDecimal.ZERO;
        return unitPrice.multiply(quantity);
    }

}
