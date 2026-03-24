package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Enum representing the status of a service ticket lifecycle.
 *
 * DRAFT:       Check-in hoàn tất, chờ kiểm tra / bắt đầu sửa
 * INSPECTION:  Đang trong quá trình kiểm tra an toàn
 * PENDING:     Chờ khách hàng duyệt báo giá / chờ phụ tùng
 * IN_PROGRESS: Đang sửa chữa
 * COMPLETED:   Kỹ thuật viên / advisor báo xong sửa xe
 * PAID:        Lễ tân xác nhận thanh toán — trigger ZNS feedback
 * CANCELLED:   Phiếu dịch vụ đã hủy
 */
public enum TicketStatus {
    DRAFT,
    INSPECTION,
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    PAID,
    CANCELLED
}
