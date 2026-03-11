package com.g42.platform.gms.service_ticket_management.api.mapper.assignment;

import com.g42.platform.gms.service_ticket_management.api.dto.assignment.AssignmentInfo;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import org.hibernate.sql.ast.tree.update.Assignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketAssignmentDtoMapper {
    @Mapping(target = "assignmentId", source = "id")
    AssignmentInfo toAssignmentInfo(ServiceTicketAssignment serviceTicketAssignment);
}
