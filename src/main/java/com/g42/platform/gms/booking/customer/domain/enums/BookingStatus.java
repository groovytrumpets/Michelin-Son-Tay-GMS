package com.g42.platform.gms.booking.customer.domain.enums;

public enum BookingStatus {
    NEW,
    DRAFT,        // Nháp
    PENDING,      // Chờ xác nhận
    CONFIRMED,    // Đã xác nhận
    CANCELLED,    // Đã hủy
    NOT_ARRIVED   // Khách không đến
}
