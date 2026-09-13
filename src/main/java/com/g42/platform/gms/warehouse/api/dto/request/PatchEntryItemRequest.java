package com.g42.platform.gms.warehouse.api.dto.request;

import lombok.Data;

import java.math.BigDecimal;

/** Sửa từng dòng item trong phiếu nhập kho */
@Data
public class PatchEntryItemRequest {
    private BigDecimal quantity;
    private BigDecimal importPrice;
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
