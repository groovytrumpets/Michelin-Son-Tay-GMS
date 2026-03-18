package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAssignmentJpaRepo extends JpaRepository<ServiceTicketAssignmentJpa, Integer> {
    boolean existsByServiceTicketId(Integer serviceTicketId);

    boolean existsByIsPrimaryAndServiceTicketId(Boolean isPrimary, Integer serviceTicketId);
}
