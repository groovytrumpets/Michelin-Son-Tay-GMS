package com.g42.platform.gms.service_ticket_management.infrastructure;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicketAssignment;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketAssignmentRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.ServiceTicketMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.TicketAssignmentJpaMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.TicketAssignmentJpaRepo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@AllArgsConstructor
public class ServiceTicketAssignmentRepoImpl implements TicketAssignmentRepo {
    private final TicketAssignmentJpaRepo ticketAssignmentJpaRepo;
    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceTicketMapper  serviceTicketMapper;
    private final TicketAssignmentJpaMapper ticketAssignmentJpaMapper;
    @Override
    public ServiceTicket findById(Integer serviceTicketId) {
        ServiceTicketJpa serviceTicketJpa =serviceTicketRepository.findByServiceTicketId(serviceTicketId);
        return serviceTicketMapper.toDomain(serviceTicketJpa);
    }

    @Override
    public List<ServiceTicketAssignment> findAssignByServiceIdAndStatus(Integer serviceTicketId, String active) {
        List<ServiceTicketAssignmentJpa> serviceTicketAssignmentJpa = ticketAssignmentJpaRepo.findByServiceTicketIdAndStatus(serviceTicketId, active);
        return serviceTicketAssignmentJpa.stream().map(ticketAssignmentJpaMapper::toDoamin).toList();
    }
}
