package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Trạng thái assignment trong service ticket.
 * Dùng để theo dõi ai đang làm việc và ai đã hoàn thành.
 */
public enum AssignmentStatus {
    /**
     * Đang chờ bắt đầu làm việc.
     * - Advisor: được assign nhưng chưa thực hiện công việc gì (chưa assign technician).
     * - Technician: được assign nhưng chưa bắt đầu kiểm tra an toàn.
     * Lễ tân có thể thay đổi advisor, advisor có thể thay đổi technician ở trạng thái này.
     */
    PENDING,
    
    /**
     * Đang được phân công và làm việc.
     * - Advisor: đã assign technician thành công và bắt đầu làm việc.
     * - Technician: đã bắt đầu kiểm tra an toàn hoặc advisor đã bấm "bắt đầu làm việc".
     * Không thể thay đổi staff ở trạng thái này.
     */
    ACTIVE,
    
    /**
     * Đã hoàn thành công việc trên ticket này.
     * Ticket đã PAID, staff có thể được assign vào ticket khác.
     */
    DONE,
    
    /**
     * Đã bị hủy assignment (ví dụ: thay đổi staff).
     * Không còn active trên ticket này.
     */
    CANCELLED
}