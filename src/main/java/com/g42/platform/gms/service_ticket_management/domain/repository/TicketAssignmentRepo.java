package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for ServiceTicketAssignment.
 * Chỉ làm việc với domain entity, không biết về DTO hay JPA.
 */
public interface TicketAssignmentRepo {

    /** Lưu assignment mới hoặc cập nhật. */
    ServiceTicketAssignment save(ServiceTicketAssignment assignment);

    /** Tìm theo assignmentId. */
    Optional<ServiceTicketAssignment> findById(Integer assignmentId);

    /** Lấy tất cả assignment của một ticket. */
    List<ServiceTicketAssignment> findByTicketId(Integer ticketId);

    /** Kiểm tra ticket đã có assignment với role cụ thể chưa. */
    boolean existsByTicketIdAndRole(Integer ticketId, String role);

    /** Kiểm tra ticket đã có primary assignment chưa. */
    boolean existsPrimaryByTicketId(Integer ticketId);

    /** Kiểm tra staff có đang rảnh không (status = CANCEL nghĩa là không bận). */
    boolean isStaffAvailable(Integer staffId);

    /** Kiểm tra staff đã được assign vào ticket cụ thể chưa. */
    boolean isStaffAssignedToTicket(Integer staffId, Integer ticketId);
}
