package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Phân loại phiếu dịch vụ.
 * SERVICE: phiếu sửa chữa tại xưởng (mặc định, đi qua đầy đủ quy trình).
 * PARTS_SALE: phiếu bán linh kiện cho đại lý/garage khác (rút gọn, bỏ qua
 * kiểm tra an toàn, trạng thái sửa xe và giao việc thủ công).
 */
public enum TicketType {
    SERVICE,
    PARTS_SALE
}
