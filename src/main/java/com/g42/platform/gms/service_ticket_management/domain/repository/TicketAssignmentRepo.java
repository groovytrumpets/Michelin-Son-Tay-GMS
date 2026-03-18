package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.RoleDto;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAssignmentRepo {
    List<AvailableStaffDto> getAvailableStaff(Integer ticketId, String role);

    AssignStaffDto assignStaff(Integer ticketId, AssignStaffDto dto);
}
