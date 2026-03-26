package com.g42.platform.gms.service_ticket_management.infrastructure.implement;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketAssignmentRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.TicketAssignmentJpaMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.TicketAssignmentJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Infrastructure implementation of TicketAssignmentRepo.
 * Chỉ xử lý persistence, không chứa business logic.
 */
@Repository
@RequiredArgsConstructor
public class ServiceTicketAssignmentRepoImpl implements TicketAssignmentRepo {

    private final TicketAssignmentJpaRepo ticketAssignmentJpaRepo;
    private final TicketAssignmentJpaMapper mapper;

    @Override
    public ServiceTicketAssignment save(ServiceTicketAssignment assignment) {
        ServiceTicketAssignmentJpa jpa = mapper.toJpa(assignment);
        return mapper.toDomain(ticketAssignmentJpaRepo.save(jpa));
    }

    @Override
    public Optional<ServiceTicketAssignment> findById(Integer assignmentId) {
        return ticketAssignmentJpaRepo.findById(assignmentId)
            .map(mapper::toDomain);
    }

    @Override
    public List<ServiceTicketAssignment> findByTicketId(Integer ticketId) {
        return ticketAssignmentJpaRepo.findByServiceTicketId(ticketId).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByTicketIdAndRole(Integer ticketId, String role) {
        return ticketAssignmentJpaRepo.existsByServiceTicketIdAndRoleInTicket(ticketId, role);
    }

    @Override
    public boolean existsPrimaryByTicketId(Integer ticketId) {
        return ticketAssignmentJpaRepo.existsByIsPrimaryAndServiceTicketId(Boolean.TRUE, ticketId);
    }

    @Override
    public boolean isStaffAvailable(Integer staffId) {
        // Staff rảnh khi không có assignment ACTIVE nào trong ticket có trạng thái bận (DRAFT, IN_PROGRESS, INSPECTION)
        return !ticketAssignmentJpaRepo.hasActiveAssignmentInBusyTickets(staffId);
    }

    @Override
    public boolean isStaffAssignedToTicket(Integer staffId, Integer ticketId) {
        return ticketAssignmentJpaRepo.existsByStaffIdAndServiceTicketId(staffId, ticketId);
    }
    
    @Override
    public List<ServiceTicketAssignment> findByTicketIdAndRole(Integer ticketId, String role) {
        return ticketAssignmentJpaRepo.findByTicketIdAndRole(ticketId, role).stream()
            .map(mapper::toDomain)
            .toList();
    }
}