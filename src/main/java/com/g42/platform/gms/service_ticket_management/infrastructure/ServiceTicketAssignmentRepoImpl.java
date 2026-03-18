package com.g42.platform.gms.service_ticket_management.infrastructure;

import com.g42.platform.gms.service_ticket_management.api.dto.assign.AssignStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.AvailableStaffDto;
import com.g42.platform.gms.service_ticket_management.api.dto.assign.RoleDto;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketAssignmentRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.TicketAssignmentJpaMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.TicketAssignmentJpaRepo;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import com.g42.platform.gms.staff.profile.infrastructure.repository.StaffProileJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
@Repository
@RequiredArgsConstructor
public class ServiceTicketAssignmentRepoImpl implements TicketAssignmentRepo {
    private final TicketAssignmentJpaRepo  ticketAssignmentJpaRepo;
    private final StaffProileJpaRepo  staffProileJpaRepo;
    private final TicketAssignmentJpaMapper  ticketAssignmentJpaMapper;
    @Override
    public List<AvailableStaffDto> getAvailableStaff(Integer ticketId, String role) {
        return staffProileJpaRepo.findAvailableStaffByRole(role).stream().map(ticketAssignmentJpaMapper::toDto).toList();
    }

    @Override
    public AssignStaffDto assignStaff(Integer ticketId, AssignStaffDto dto) {
        if ("ADVISOR".equals(dto.getRoleInTicket())) {
            if (ticketAssignmentJpaRepo.existsByServiceTicketId(ticketId)) {
                throw new RuntimeException("Ticket đã có advisor!");
            }
        }
        if (Boolean.TRUE.equals(dto.getIsPrimary())) {
            if (ticketAssignmentJpaRepo.existsByIsPrimaryAndServiceTicketId(Boolean.TRUE,ticketId)) {
                throw new RuntimeException("Ticket đã có technician chính!");
            }
        }
        //todo: create new assgin
        ServiceTicketAssignmentJpa sa = new ServiceTicketAssignmentJpa();
        sa.setServiceTicketId(ticketId);
        sa.setStaffId(dto.getStaffId());
        sa.setRoleInTicket(dto.getRoleInTicket());
        sa.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
        sa.setNote(dto.getNote());
        sa.setAssignedAt(Instant.now());
        sa.setStatus("ACTIVE");

        ServiceTicketAssignmentJpa savedSa = ticketAssignmentJpaRepo.save(sa);
        return ticketAssignmentJpaMapper.toAssginDto(savedSa);
    }

    @Override
    public AssignStaffDto updateAssignment(Integer ticketId, Integer assignmentId, AssignStaffDto dto) {
        if ("ADVISOR".equals(dto.getRoleInTicket())) {
            if (ticketAssignmentJpaRepo.existsByServiceTicketId(ticketId)) {
                throw new RuntimeException("Ticket đã có advisor!");
            }
        }
        if (Boolean.TRUE.equals(dto.getIsPrimary())) {
            if (ticketAssignmentJpaRepo.existsByIsPrimaryAndServiceTicketId(Boolean.TRUE,ticketId)) {
                throw new RuntimeException("Ticket đã có technician chính!");
            }
        }
        ServiceTicketAssignmentJpa sa = ticketAssignmentJpaRepo.findByAssignmentId(assignmentId);
        if (sa == null) {
            throw new RuntimeException("Assignment not found!");
        }
        if (dto.getStaffId() != null) sa.setStaffId(dto.getStaffId());
        if (dto.getRoleInTicket() != null) sa.setRoleInTicket(dto.getRoleInTicket());
        if (dto.getIsPrimary() != null) sa.setIsPrimary(dto.getIsPrimary());
        if (dto.getNote() != null) sa.setNote(dto.getNote());
        ServiceTicketAssignmentJpa savedSa = ticketAssignmentJpaRepo.save(sa);
        return ticketAssignmentJpaMapper.toAssginDto(savedSa);
    }
}
