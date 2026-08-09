package com.g42.platform.gms.estimation.domain.enums;

/**
 * Bốn bên được phân bổ hoa hồng trên một phiếu báo giá.
 * CUSTOMER: hoa hồng trả lại cho khách hàng.
 * ADJUSTMENT: chi phí điều chỉnh, ghi nhận như một khoản giảm doanh thu.
 * ASSESSOR: hoa hồng giám định viên.
 * BROKER: hoa hồng môi giới giới thiệu khách.
 */
public enum CommissionPartyType {
    CUSTOMER,
    ADJUSTMENT,
    ASSESSOR,
    BROKER
}
