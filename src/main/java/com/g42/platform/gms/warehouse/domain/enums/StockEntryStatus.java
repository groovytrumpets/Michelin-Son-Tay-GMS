package com.g42.platform.gms.warehouse.domain.enums;

public enum StockEntryStatus {
    DRAFT,
    CONFIRMED,
    /** Phiếu DRAFT bị huỷ (xoá mềm) — chưa từng cộng tồn, khôi phục được ở thùng rác. */
    CANCELLED
}
