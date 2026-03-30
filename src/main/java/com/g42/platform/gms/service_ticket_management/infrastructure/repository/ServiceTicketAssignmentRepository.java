package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceTicketAssignmentRepository extends JpaRepository<ServiceTicketAssignmentJpa, Integer> {

    List<ServiceTicketAssignmentJpa> findByServiceTicketId(Integer serviceTicketId);

    List<ServiceTicketAssignmentJpa> findByStaffId(Integer staffId);

    /**
     * Đếm số service ticket COMPLETED của staff trong tháng/năm cụ thể.
     */
    @Query("SELECT COUNT(a) FROM ServiceTicketAssignmentJpa a " +
           "JOIN ServiceTicketManagement t ON t.serviceTicketId = a.serviceTicketId " +
           "WHERE a.staffId = :staffId " +
           "AND t.ticketStatus = 'COMPLETED' " +
           "AND YEAR(t.completedAt) = :year AND MONTH(t.completedAt) = :month")
    long countCompletedByStaffInMonth(@Param("staffId") Integer staffId,
                                      @Param("year") int year,
                                      @Param("month") int month);

    @Query("SELECT COUNT(a) FROM ServiceTicketAssignmentJpa a " +
           "JOIN ServiceTicketManagement t ON t.serviceTicketId = a.serviceTicketId " +
           "WHERE a.staffId = :staffId AND a.roleInTicket = :role " +
           "AND t.ticketStatus = 'COMPLETED' " +
           "AND YEAR(t.completedAt) = :year AND MONTH(t.completedAt) = :month")
    long countCompletedByStaffAndRoleInMonth(@Param("staffId") Integer staffId,
                                             @Param("role") String role,
                                             @Param("year") int year,
                                             @Param("month") int month);
}
