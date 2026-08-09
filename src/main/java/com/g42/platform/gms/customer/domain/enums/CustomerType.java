package com.g42.platform.gms.customer.domain.enums;

/**
 * Phân loại đối tượng trong danh bạ đối tác.
 * INDIVIDUAL: khách lẻ (mặc định).
 * DEALER: đại lý mua linh kiện.
 * GARAGE: garage khác mua linh kiện.
 * ASSESSOR: giám định viên, nhận hoa hồng GĐV trên phiếu báo giá.
 * BROKER: môi giới giới thiệu khách, nhận hoa hồng môi giới.
 * VENDOR: nhà cung cấp nhận việc thuê ngoài trên dòng báo giá.
 */
public enum CustomerType {
    INDIVIDUAL,
    DEALER,
    GARAGE,
    ASSESSOR,
    BROKER,
    VENDOR
}
