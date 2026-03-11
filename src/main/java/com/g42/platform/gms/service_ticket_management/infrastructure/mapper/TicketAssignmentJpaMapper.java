package com.g42.platform.gms.service_ticket_management.infrastructure.mapper;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketAssignmentJpaMapper {
    ServiceTicketAssignment toDoamin (ServiceTicketAssignmentJpa serviceTicketAssignmentJpa);

}
