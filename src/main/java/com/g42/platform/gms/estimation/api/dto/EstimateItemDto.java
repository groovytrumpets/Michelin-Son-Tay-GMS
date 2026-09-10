package com.g42.platform.gms.estimation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateItemDto {
    private Integer estimateItemId;
    private String itemName;
    private ItemCateDto itemCategory;
    /** Nhãn hạng mục gõ tay của riêng dòng; ưu tiên hiển thị hơn tên danh mục. */
    private String categoryLabel;
    private Integer warehouseId;
    private Integer itemId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal;
    private BigDecimal taxAmount;
    private BigDecimal appliedTaxRate;
    private String taxCode;
    private Boolean isChecked;
    private Boolean isRemoved;
    private BigDecimal unitPriceWithVat;
    private BigDecimal subTotalWithVat;
    private String unit;
    private Integer revisedFromItemId;
    private Integer promotionId;
    private Boolean isGift;
    private Integer triggeredByItemId;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    /** True khi THÀNH TIỀN của dòng được advisor khoá tay. */
    private Boolean isOverridden;
    /** THÀNH TIỀN gõ tay đang áp cho dòng; null nghĩa là tính tự động. */
    private BigDecimal manualLineTotal;
    private WarehouseDto warehouse;
    private StockAllocationDto stockAllocation;
    private Integer entryItemId;
    private BigDecimal importPrice;
    private Boolean isOutsource;
    private Integer outsourcePartnerId;
    /** Tên đối tác thuê ngoài, tra từ danh bạ để hiển thị thẳng trên bảng báo giá. */
    private String outsourcePartnerName;
    private String outsourceWorkContent;
    private BigDecimal laborCost;
    private String note;
    private BigDecimal discountPercent;
}
