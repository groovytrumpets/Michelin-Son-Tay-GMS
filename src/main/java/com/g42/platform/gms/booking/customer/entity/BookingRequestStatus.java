package com.g42.platform.gms.booking.customer.entity;

public enum BookingRequestStatus {
    PENDING,    // Chờ nhân viên xác nhận
    CONFIRMED,  // Đã xác nhận và tạo Booking
    REJECTED,   // Nhân viên từ chối
    EXPIRED     // Hết hạn sau 24h
}