package com.g42.platform.gms.service_ticket_management.infrastructure.implement;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.ServiceTicketMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.specification.ServiceTicketSpecification;
import com.g42.platform.gms.service_ticket_management.infrastructure.specification.WorkHistorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ServiceTicketRepoImpl implements ServiceTicketRepo {

    private final ServiceTicketRepository jpaRepo;
    private final ServiceTicketMapper mapper;

    @Override
    public Optional<ServiceTicket> findByTicketCode(String ticketCode) {
        return jpaRepo.findByTicketCode(ticketCode).map(mapper::toDomain);
    }

    @Override
    public Optional<ServiceTicket> findByBookingId(Integer bookingId) {
        return jpaRepo.findByBookingId(bookingId).map(mapper::toDomain);
    }

    @Override
    public ServiceTicket findByServiceTicketId(Integer serviceTicketId) {
        ServiceTicketJpa jpa = jpaRepo.findByServiceTicketId(serviceTicketId);
        if (jpa == null) return null;
        return mapper.toDomain(jpa);
    }

    @Override
    public boolean existsByTicketCode(String ticketCode) {
        return jpaRepo.existsByTicketCode(ticketCode);
    }

    @Override
    public ServiceTicket save(ServiceTicket ticket) {
        ServiceTicketJpa jpa = mapper.toJpa(ticket);
        // Preserve ID for updates
        if (ticket.getServiceTicketId() != null) {
            jpa.setServiceTicketId(ticket.getServiceTicketId());
        }
        return mapper.toDomain(jpaRepo.save(jpa));
    }

    @Override
    public void deleteById(Integer id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public List<ServiceTicket> findAll() {
        return jpaRepo.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<ServiceTicket> findAll(TicketStatus status, LocalDate date, String search, Pageable pageable) {
        Specification<ServiceTicketJpa> spec = ServiceTicketSpecification.filter(date, status);
        if (search != null && !search.isBlank()) {
            spec = spec.and(ServiceTicketSpecification.search(search));
        }
        return jpaRepo.findAll(spec, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<ServiceTicket> findByAssignedStaff(Integer staffId, TicketStatus status, LocalDate date, String search, Pageable pageable) {
        Specification<ServiceTicketJpa> spec = ServiceTicketSpecification.assignedToStaff(staffId)
                .and(ServiceTicketSpecification.filter(date, status));
        if (search != null && !search.isBlank()) {
            spec = spec.and(ServiceTicketSpecification.search(search));
        }
        return jpaRepo.findAll(spec, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<ServiceTicket> findByTechnicianCompleted(Integer technicianId, LocalDate startDate, LocalDate endDate, String licensePlate, Pageable pageable) {
        Specification<ServiceTicketJpa> spec = Specification.where(WorkHistorySpecification.byTechnicianId(technicianId))
                .and(WorkHistorySpecification.isCompleted())
                .and(WorkHistorySpecification.completedBetween(startDate, endDate));
        if (licensePlate != null && !licensePlate.isBlank()) {
            spec = spec.and(WorkHistorySpecification.byLicensePlate(licensePlate));
        }
        return jpaRepo.findAll(spec, pageable).map(mapper::toDomain);
    }

    @Override
    public List<ServiceTicket> findByVehicleId(Integer vehicleId) {
        return jpaRepo.findByVehicleId(vehicleId).stream().map(mapper::toDomain).toList();
    }
}
