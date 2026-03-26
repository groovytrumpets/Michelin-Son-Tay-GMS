package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAssignmentJpaRepo extends JpaRepository<ServiceTicketAssignmentJpa, Integer> {

    List<ServiceTicketAssignmentJpa> findByServiceTicketId(Integer serviceTicketId);

    boolean existsByServiceTicketId(Integer serviceTicketId);

    boolean existsByServiceTicketIdAndRoleInTicket(Integer serviceTicketId, String roleInTicket);

    boolean existsByIsPrimaryAndServiceTicketId(Boolean isPrimary, Integer serviceTicketId);

    ServiceTicketAssignmentJpa findByAssignmentId(Integer assignmentId);

    ServiceTicketAssignmentJpa findByStaffIdAndStatus(Integer staffId, String status);

    boolean existsByStaffIdAndServiceTicketId(Integer staffId, Integer serviceTicketId);
}
