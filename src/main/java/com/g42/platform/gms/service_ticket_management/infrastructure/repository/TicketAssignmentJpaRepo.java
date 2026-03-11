package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAssignmentJpaRepo extends JpaRepository<ServiceTicketAssignment,Integer> {
    List<ServiceTicketAssignmentJpa> findByServiceTicketIdAndStatus(Integer serviceTicketId, String status);
}
