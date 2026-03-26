package com.g42.platform.gms.service_ticket_management.api.mapper.assignment;

import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import org.mapstruct.Mapper;

/**
 * API mapper: ServiceTicketAssignment (domain entity) ↔ AssignStaffDto (API DTO).
 * Dùng trong application/service layer để trả response về controller.
 */
@Mapper(componentModel = "spring")
public interface TicketAssignmentDtoMapper {

    AssignStaffDto toDto(ServiceTicketAssignment domain);

    ServiceTicketAssignment toDomain(AssignStaffDto dto);
}
