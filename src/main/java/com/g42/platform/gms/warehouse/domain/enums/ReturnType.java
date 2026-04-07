package com.g42.platform.gms.warehouse.domain.enums;

/**
 * Loại phiếu hoàn hàng:
 * - CUSTOMER_RETURN: khách trả hàng lỗi → cộng inventory khi confirm
 * - SUPPLIER_RETURN: trả hàng về NCC → trừ inventory khi confirm
 * - EXCHANGE: đổi hàng → cộng hàng lỗi + trừ hàng mới xuất
 */
public enum ReturnType {
    CUSTOMER_RETURN,
    SUPPLIER_RETURN,
    EXCHANGE
}
