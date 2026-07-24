package com.g42.platform.gms.customer.domain.enums;

/**
 * Hạng khách hàng dựa trên điểm tích lũy trong năm.
 */
public enum CustomerRank {
    BRONZE,   // Đồng  - mặc định, 0+ điểm
    SILVER,   // Bạc   - 5.000+ điểm
    GOLD,     // Vàng  - 15.000+ điểm
    PLATINUM, // Bạch Kim - 30.000+ điểm
    DIAMOND   // Kim Cương - 50.000+ điểm
}
