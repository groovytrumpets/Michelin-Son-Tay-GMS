package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Trạng thái vòng đời phiếu dịch vụ.
 *
 * CREATED:    Lễ tân tạo phiếu, chờ kỹ thuật viên bắt đầu kiểm tra
 * INSPECTING: Kỹ thuật viên đang kiểm tra an toàn xe
 * INSPECTED:  Kỹ thuật viên hoàn thành kiểm tra, chờ advisor lập báo giá
 * ESTIMATED:  Advisor đã lập báo giá, chờ khách duyệt / chờ phụ tùng
 * PENDING:    Chờ phụ tùng hoặc dịch vụ không khả dụng
 * REPAIRING:  Đang sửa chữa / thực hiện dịch vụ
 * COMPLETED:  Kỹ thuật viên báo xong, chờ lễ tân xác nhận thanh toán
 * PAID:       Lễ tân xác nhận thanh toán — trigger ZNS feedback
 * CANCELLED:  Phiếu dịch vụ đã hủy
 */
public enum TicketStatus {
    CREATED,
    INSPECTING,
    INSPECTED,
    ESTIMATED,
    PENDING,
    REPAIRING,
    COMPLETED,
    PAID,
    CANCELLED
}
