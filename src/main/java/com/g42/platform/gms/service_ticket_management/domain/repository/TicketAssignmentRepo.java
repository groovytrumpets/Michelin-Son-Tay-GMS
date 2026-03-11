package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAssignmentRepo {
    ServiceTicket findById(Integer serviceTicketId);

    List<ServiceTicketAssignment> findAssignByServiceIdAndStatus(Integer serviceTicketId, String active);
}
