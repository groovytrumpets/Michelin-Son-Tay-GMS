package com.g42.platform.gms.warehouse.domain.enums;

/**
 * Phân loại lý do hoàn hàng.
 *
 * WRONG_TYPE   – xuất nhầm kiểu / mẫu (sản phẩm còn nguyên vẹn, trả về kho thường)
 * DEFECTIVE    – hàng bị lỗi (trả vào kho hàng lỗi riêng, cần ghi trách nhiệm)
 */
public enum ReturnReason {
    WRONG_TYPE,
    DEFECTIVE
}
