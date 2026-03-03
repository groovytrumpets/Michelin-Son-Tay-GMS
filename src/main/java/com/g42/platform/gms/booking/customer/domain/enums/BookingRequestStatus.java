package com.g42.platform.gms.booking.customer.domain.enums;

public enum BookingRequestStatus {
    PENDING,    // Chờ nhân viên xác nhận
    CONFIRMED,  // Đã xác nhận và tạo Booking
    REJECTED,   // Nhân viên từ chối
    EXPIRED,
    CONTACTED,
    SPAM// Hết hạn sau 24h
}
