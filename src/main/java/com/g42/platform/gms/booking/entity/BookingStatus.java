package com.g42.platform.gms.booking.entity;

public enum BookingStatus {
    NEW,            // Khách mới đặt online (Chưa có thông tin xe)
    CONFIRMED,      // Gara đã xác nhận qua điện thoại
    CHECKED_IN,     // Khách đã đến nơi -> Nhân viên cập nhật xe vào lúc này
    COMPLETED,      // Làm xong
    CANCELLED       // Hủy
}