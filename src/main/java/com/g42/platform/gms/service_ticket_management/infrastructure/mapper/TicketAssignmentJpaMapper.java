package com.g42.platform.gms.service_ticket_management.infrastructure.mapper;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import org.mapstruct.Mapper;

/**
 * Infrastructure mapper: ServiceTicketAssignmentJpa ↔ ServiceTicketAssignment (domain entity).
 * Chỉ map giữa JPA entity và domain entity, không biết về DTO.
 */
@Mapper(componentModel = "spring")
public interface TicketAssignmentJpaMapper {

    ServiceTicketAssignment toDomain(ServiceTicketAssignmentJpa jpa);

    ServiceTicketAssignmentJpa toJpa(ServiceTicketAssignment domain);
}
