package com.g42.platform.gms.service_ticket_management.infrastructure.mapper;

import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TicketAssignmentJpaMapper {
    AvailableStaffDto toDto (StaffProfileJpa staffProfileJpa);
    AssignStaffDto toAssginDto (ServiceTicketAssignmentJpa serviceTicketAssignmentJpa);
}
