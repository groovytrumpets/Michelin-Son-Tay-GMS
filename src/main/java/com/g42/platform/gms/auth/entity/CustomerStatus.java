package com.g42.platform.gms.auth.entity;

public enum CustomerStatus {
    ACTIVE,    // Đã có PIN, đăng nhập bình thường
    LOCKED,    // Bị khóa do nhập sai quá nhiều lần
    INACTIVE   // [MỚI] Lễ tân đã tạo, nhưng khách chưa kích hoạt (Chưa có PIN)
}