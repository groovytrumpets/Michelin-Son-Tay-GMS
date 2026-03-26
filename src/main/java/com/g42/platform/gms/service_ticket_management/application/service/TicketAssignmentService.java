package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.api.mapper.assignment.TicketAssignmentDtoMapper;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.domain.exception.AssignmentErrorCode;
import com.g42.platform.gms.service_ticket_management.domain.exception.AssignmentException;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketAssignmentRepo;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Application service cho ticket assignment.
 * Business logic nằm ở đây, không trong repo impl.
 */
@Service
@RequiredArgsConstructor
public class TicketAssignmentService {

    private final TicketAssignmentRepo ticketAssignmentRepo;
    private final TicketAssignmentDtoMapper dtoMapper;
    private final StaffProileJpaRepo staffProfileRepo;

    @Transactional(readOnly = true)
    public List<AvailableStaffDto> getAvailableStaff(Integer ticketId, String role) {
        return staffProfileRepo.findAvailableStaffByRole(role, ticketId).stream()
            .map(this::toAvailableStaffDto)
            .toList();
    }

    @Transactional
    public AssignStaffDto assignStaff(Integer ticketId, AssignStaffDto dto) {
        // Validate: mỗi ticket chỉ có 1 advisor
        boolean isAdvisorRole = "ADVISOR".equals(dto.getRoleInTicket());
        if (isAdvisorRole) {
            if (ticketAssignmentRepo.existsByTicketIdAndRole(ticketId, "ADVISOR")) {
                throw new AssignmentException("Ticket đã có advisor!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
            }
        }

        // Validate: mỗi ticket chỉ có 1 primary technician
        boolean wantPrimary = dto.getIsPrimary() != null && dto.getIsPrimary();
        if (wantPrimary) {
            if (ticketAssignmentRepo.existsPrimaryByTicketId(ticketId)) {
                throw new AssignmentException("Ticket đã có technician chính!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID);
            }
        }

        // Validate: staff phải rảnh (chỉ áp dụng cho TECHNICIAN, advisor không giới hạn)
        // Validate: staff phải có đúng role tương ứng với roleInTicket
        boolean staffHasRole = staffProfileRepo.existsByStaffIdAndRole(dto.getStaffId(), dto.getRoleInTicket());
        if (!staffHasRole) {
            throw new AssignmentException("Staff không có role " + dto.getRoleInTicket(), AssignmentErrorCode.UNAVAILABLE_STAFF);
        }

        if (!isAdvisorRole && !ticketAssignmentRepo.isStaffAvailable(dto.getStaffId())) {
            throw new AssignmentException("Staff đang bận!", AssignmentErrorCode.UNAVAILABLE_STAFF);
        }

        ServiceTicketAssignment assignment = new ServiceTicketAssignment();
        assignment.setServiceTicketId(ticketId);
        assignment.setStaffId(dto.getStaffId());
        assignment.setRoleInTicket(dto.getRoleInTicket());

        boolean isPrimary = dto.getIsPrimary() != null && dto.getIsPrimary();
        assignment.setIsPrimary(isPrimary);
        assignment.setNote(dto.getNote());
        assignment.setAssignedAt(Instant.now());
        assignment.setStatus("ACTIVE");

        ServiceTicketAssignment saved = ticketAssignmentRepo.save(assignment);
        return dtoMapper.toDto(saved);
    }

    @Transactional
    public AssignStaffDto updateAssignment(Integer ticketId, Integer assignmentId, AssignStaffDto dto) {
        ServiceTicketAssignment existing = ticketAssignmentRepo.findById(assignmentId)
            .orElseThrow(() -> new AssignmentException("Assignment không tồn tại!", AssignmentErrorCode.INVALID_SERVICE_TICKET_ID));

        if (dto.getStaffId() != null) existing.setStaffId(dto.getStaffId());
        if (dto.getRoleInTicket() != null) existing.setRoleInTicket(dto.getRoleInTicket());
        if (dto.getIsPrimary() != null) existing.setIsPrimary(dto.getIsPrimary());
        if (dto.getNote() != null) existing.setNote(dto.getNote());

        ServiceTicketAssignment saved = ticketAssignmentRepo.save(existing);
        return dtoMapper.toDto(saved);
    }

    private AvailableStaffDto toAvailableStaffDto(StaffProfileJpa staff) {
        AvailableStaffDto dto = new AvailableStaffDto();
        dto.setStaffId(staff.getStaffId());
        dto.setFullName(staff.getFullName());
        dto.setPhone(staff.getPhone());
        dto.setAvatar(staff.getAvatar());
        return dto;
    }
}
