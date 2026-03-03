package com.g42.platform.gms.booking_management.domain.enums;

public enum BookingEnum {
    DONE,
    NEW,
    DRAFT,        // Nháp
    PENDING,      // Chờ xác nhận
    CONFIRMED,    // Đã xác nhận
    CANCELLED,    // Đã hủy
    NOT_ARRIVED,  // Khách không đến
}
