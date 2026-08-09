package com.g42.platform.gms.warehouse.domain.enums;

/**
 * Chiến lược chọn lô khi thêm vật tư vào bảng báo giá.
 * FIFO: lô nhập trước xuất trước.
 * LIFO: lô nhập sau xuất trước.
 * FEFO: lô hết hạn trước xuất trước; lô không có hạn dùng xếp sau cùng.
 * MANUAL: không tự chọn lô, để người dùng bấm chọn.
 */
public enum StockAllocationMethod {
    FIFO,
    LIFO,
    FEFO,
    MANUAL
}
