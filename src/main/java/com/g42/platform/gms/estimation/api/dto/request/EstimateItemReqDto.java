package com.g42.platform.gms.estimation.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateItemReqDto {
    private Integer estimateItemId;
    /** Danh mục có sẵn; để trống là dòng không xếp nhóm — hoàn toàn hợp lệ. */
    private Integer itemCategoryId;
    /** Tên nhóm gõ tay, chỉ áp cho riêng dòng này và không tạo danh mục mới. */
    private String categoryLabel;
    private Integer itemId;
    private Integer warehouseId;
    private String itemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Integer taxRuleId;
    private Boolean isChecked;
    private Boolean isRemoved;
    private String unit;
    private Integer revisedFromItemId;
    private Integer promotionId;
    private Boolean isGift;
    private Integer triggeredByItemId;
    private BigDecimal discountAmount;
//    private BigDecimal finalPrice;
    /** True khi advisor khoá THÀNH TIỀN bằng tay cho dòng này. */
    private Boolean isOverridden;
    /** THÀNH TIỀN gõ tay; khi có, backend dùng thẳng số này thay vì SL x đơn giá x thuế. */
    private BigDecimal manualLineTotal;
    private Integer entryItemId;
    private Boolean isOutsource;
    private Integer outsourcePartnerId;
    private String outsourceWorkContent;
    private BigDecimal laborCost;
    private String note;
    private BigDecimal discountPercent;
}
