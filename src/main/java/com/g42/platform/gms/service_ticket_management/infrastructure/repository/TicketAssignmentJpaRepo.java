package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.domain.enums.AssignmentStatus;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketAssignmentJpaRepo extends JpaRepository<ServiceTicketAssignmentJpa, Integer> {

    List<ServiceTicketAssignmentJpa> findByServiceTicketId(Integer serviceTicketId);

    boolean existsByServiceTicketId(Integer serviceTicketId);

    boolean existsByServiceTicketIdAndRoleInTicket(Integer serviceTicketId, String roleInTicket);

    boolean existsByIsPrimaryAndServiceTicketId(Boolean isPrimary, Integer serviceTicketId);

    ServiceTicketAssignmentJpa findByAssignmentId(Integer assignmentId);

    ServiceTicketAssignmentJpa findByStaffIdAndStatus(Integer staffId, AssignmentStatus status);

    boolean existsByStaffIdAndServiceTicketId(Integer staffId, Integer serviceTicketId);

    /**
     * Check if staff has any active assignment in tickets with busy status (DRAFT, IN_PROGRESS, INSPECTION)
     */
    @Query("""
        SELECT COUNT(sta) > 0 FROM ServiceTicketAssignmentJpa sta
        JOIN ServiceTicketManagement st ON st.serviceTicketId = sta.serviceTicketId
        WHERE sta.staffId = :staffId
        AND (sta.status = 'ACTIVE' OR sta.status = 'PENDING')
        AND (st.ticketStatus = 'DRAFT' OR st.ticketStatus = 'IN_PROGRESS' OR st.ticketStatus = 'INSPECTION')
    """)
    boolean hasActiveAssignmentInBusyTickets(@Param("staffId") Integer staffId);

    /**
     * Tìm assignment theo ticket và role (bao gồm cả PENDING và ACTIVE)
     */
    @Query("SELECT sta FROM ServiceTicketAssignmentJpa sta WHERE sta.serviceTicketId = :ticketId AND sta.roleInTicket = :role AND (sta.status = 'ACTIVE' OR sta.status = 'PENDING')")
    List<ServiceTicketAssignmentJpa> findByTicketIdAndRole(@Param("ticketId") Integer ticketId, @Param("role") String role);
    
    /**
     * Tìm tất cả assignment của ticket
     */
    @Query("SELECT sta FROM ServiceTicketAssignmentJpa sta WHERE sta.serviceTicketId = :ticketId")
    List<ServiceTicketAssignmentJpa> findByTicketId(@Param("ticketId") Integer ticketId);
}