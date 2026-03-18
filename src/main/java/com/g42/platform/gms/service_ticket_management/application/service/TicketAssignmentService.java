package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.RoleDto;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketAssignmentRepo;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketAssignmentService {
    private final TicketAssignmentRepo ticketAssignmentRepo;
    public List<AvailableStaffDto> getAvailableStaff(Integer ticketId, String role) {
        return ticketAssignmentRepo.getAvailableStaff(ticketId, role);
    }

    public AssignStaffDto assignStaff(Integer ticketId, AssignStaffDto dto) {
        return ticketAssignmentRepo.assignStaff(ticketId, dto);
    }

    public AssignStaffDto updateAssignment(Integer ticketId, Integer assignmentId, AssignStaffDto dto) {
        return ticketAssignmentRepo.updateAssignment(ticketId, assignmentId, dto);
    }
}
