package com.g42.platform.gms.service_ticket_management.infrastructure.mapper;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Infrastructure mapper: ServiceTicketAssignmentJpa ↔ ServiceTicketAssignment (domain entity).
 * Chỉ map giữa JPA entity và domain entity, không biết về DTO.
 */
@Mapper(componentModel = "spring")
public interface TicketAssignmentJpaMapper {

    @Mapping(target = "ticketCode", source = "serviceTicket.ticketCode")
    @Mapping(target = "ticketStatus", source = "serviceTicket.ticketStatus")
    ServiceTicketAssignment toDomain(ServiceTicketAssignmentJpa jpa);

    @Mapping(target = "serviceTicket", ignore = true)
    ServiceTicketAssignmentJpa toJpa(ServiceTicketAssignment domain);
}
