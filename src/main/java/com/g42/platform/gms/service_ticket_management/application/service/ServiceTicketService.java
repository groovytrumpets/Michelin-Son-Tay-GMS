package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.estimation.api.internal.EstimateInternalApi;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Shared service for Service Ticket CRUD operations.
 * Used by all phases: check-in, advisor, repair, delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceTicketService {

    private final ServiceTicketRepo serviceTicketRepo;
    private final ServiceTicketCodeGenerator ticketCodeGenerator;
    private final com.g42.platform.gms.booking.customer.domain.repository.BookingRepository bookingRepository;
    private final EstimateInternalApi estimateInternalApi;

    /**
     * Create new service ticket using booking_code as ticket_code.
     */
    @Transactional
    public ServiceTicket createServiceTicket(Integer bookingId, Integer vehicleId, Integer customerId, Integer createdBy) {
        log.info("Creating service ticket for booking: {}, vehicle: {}, customer: {}, createdBy: {}",
            bookingId, vehicleId, customerId, createdBy);

        com.g42.platform.gms.booking.customer.domain.entity.Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        String ticketCode = booking.getBookingCode();
        log.info("Using booking_code as ticket_code: {}", ticketCode);

        ServiceTicket ticket = new ServiceTicket();
        ticket.setTicketCode(ticketCode);
        ticket.setBookingId(bookingId);
        ticket.setVehicleId(vehicleId);
        ticket.setCustomerId(customerId);
        ticket.setCreatedBy(createdBy);
        ticket.setCustomerRequest(booking.getDescription());
        ticket.initializeDefaults();

        ServiceTicket saved = serviceTicketRepo.save(ticket);
        if (booking.getEstimateId() != null) {
            estimateInternalApi.linkEstimateToServiceTicket(booking.getEstimateId(), saved.getServiceTicketId());
        }
        log.info("Created service ticket with code: {}", ticketCode);
        return saved;
    }

    @Transactional(readOnly = true)
    public ServiceTicket findByTicketCode(String ticketCode) {
        return serviceTicketRepo.findByTicketCode(ticketCode)
            .orElseThrow(() -> new RuntimeException("Service ticket not found: " + ticketCode));
    }

    @Transactional(readOnly = true)
    public Optional<ServiceTicket> findByTicketCodeOptional(String ticketCode) {
        return serviceTicketRepo.findByTicketCode(ticketCode);
    }

    @Transactional(readOnly = true)
    public ServiceTicket findByBookingId(Integer bookingId) {
        return serviceTicketRepo.findByBookingId(bookingId)
            .orElseThrow(() -> new RuntimeException("Service ticket not found for booking: " + bookingId));
    }

    @Transactional
    public ServiceTicket updateServiceTicket(ServiceTicket ticket) {
        ServiceTicket existing = findByTicketCode(ticket.getTicketCode());
        if (Boolean.TRUE.equals(existing.getImmutable())) {
            throw new RuntimeException("Cannot update immutable service ticket: " + ticket.getTicketCode());
        }
        ticket.setServiceTicketId(existing.getServiceTicketId());
        return serviceTicketRepo.save(ticket);
    }

    @Transactional
    public ServiceTicket updateStatus(String ticketCode, TicketStatus newStatus) {
        ServiceTicket ticket = findByTicketCode(ticketCode);
        ticket.setTicketStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        return updateServiceTicket(ticket);
    }

    @Transactional
    public ServiceTicket markAsImmutable(String ticketCode) {
        ServiceTicket ticket = findByTicketCode(ticketCode);
        ticket.setUpdatedAt(LocalDateTime.now());
        return updateServiceTicket(ticket);
    }

    @Transactional(readOnly = true)
    public List<ServiceTicket> findAll() {
        return serviceTicketRepo.findAll();
    }

    @Transactional(readOnly = true)
    public boolean existsByTicketCode(String ticketCode) {
        return serviceTicketRepo.existsByTicketCode(ticketCode);
    }
}
