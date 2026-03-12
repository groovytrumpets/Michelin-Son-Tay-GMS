package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA Repository for ServiceTicketAssignment entity.
 * 
 * Provides CRUD operations and custom query methods for service ticket assignments.
 * This repository is used to query which staff members are assigned to service tickets
 * and their roles (RECEPTIONIST, ADVISOR, TECHNICIAN, INSPECTOR).
 * 
 * @see ServiceTicketAssignmentJpa
 */
@Repository
public interface ServiceTicketAssignmentRepository extends JpaRepository<ServiceTicketAssignmentJpa, Integer> {
    
    // Custom query methods can be added here as needed
    // For example:
    // List<ServiceTicketAssignmentJpa> findByServiceTicketId(Integer serviceTicketId);
    // List<ServiceTicketAssignmentJpa> findByStaffIdAndRoleInTicket(Integer staffId, RoleInTicket role);
}
