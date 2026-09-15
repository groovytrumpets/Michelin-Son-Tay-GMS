package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Cách phiếu được đưa vào hệ thống.
 * NORMAL:   tạo theo luồng thường (đặt lịch/check-in/bán hàng tại quầy).
 * BACKFILL: nhập bù phiếu của ngày trước — phải qua quản lý duyệt mới xuất kho + ghi doanh thu.
 */
public enum EntryMode {
    NORMAL,
    BACKFILL
}
