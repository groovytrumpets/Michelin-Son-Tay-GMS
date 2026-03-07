package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.common.enums.CodePrefix;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.ServiceTicketMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private final ServiceTicketRepository serviceTicketRepository;
    private final ServiceTicketMapper serviceTicketMapper;
    private final ServiceTicketCodeGenerator ticketCodeGenerator;
    private final com.g42.platform.gms.booking.customer.domain.repository.BookingRepository bookingRepository;

    /**
     * Create new service ticket using booking_code as ticket_code.
     * KHÔNG generate mã mới - reuse booking_code để đảm bảo đồng bộ.
     * Database constraint uk_booking_id ensures 1-to-1 relationship (1 booking = 1 service ticket).
     * 
     * Flow:
     * 1. Query booking để lấy booking_code và description
     * 2. Reuse booking_code làm ticket_code
     * 3. Copy booking.description sang customer_request
     * 4. Save ServiceTicket với status = DRAFT
     * 
     * @param bookingId Booking ID
     * @param vehicleId Vehicle ID
     * @param customerId Customer ID
     * @param createdBy Staff ID who creates the ticket
     * @return Created ServiceTicket entity
     */
    @Transactional
    public ServiceTicket createServiceTicket(Integer bookingId, Integer vehicleId, Integer customerId, Integer createdBy) {
        log.info("Creating service ticket for booking: {}, vehicle: {}, customer: {}, createdBy: {}", 
            bookingId, vehicleId, customerId, createdBy);
        
        // Fetch booking to get booking_code and customer_request
        com.g42.platform.gms.booking.customer.domain.entity.Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        
        // Use booking_code as ticket_code (no need to generate new code)
        String ticketCode = booking.getBookingCode();
        log.info("Using booking_code as ticket_code: {}", ticketCode);
        
        // Create domain entity
        ServiceTicket ticket = new ServiceTicket();
        ticket.setTicketCode(ticketCode);
        ticket.setBookingId(bookingId);
        ticket.setVehicleId(vehicleId);
        ticket.setCustomerId(customerId);
        ticket.setCreatedBy(createdBy); // Set staff who creates the ticket
        ticket.setCustomerRequest(booking.getDescription()); // Set customer request from booking
        ticket.initializeDefaults();
        
        // Convert to JPA and save
        // Note: Database constraint uk_booking_id will prevent duplicate service tickets for same booking
        ServiceTicketJpa jpa = serviceTicketMapper.toJpa(ticket);
        ServiceTicketJpa saved = serviceTicketRepository.save(jpa);
        
        log.info("Created service ticket with code: {}", ticketCode);
        return serviceTicketMapper.toDomain(saved);
    }

    /**
     * Find service ticket by ticket code.
     * 
     * @param ticketCode Ticket code (MST_XXXXXX)
     * @return ServiceTicket entity
     */
    @Transactional(readOnly = true)
    public ServiceTicket findByTicketCode(String ticketCode) {
        log.info("Finding service ticket: {}", ticketCode);
        
        ServiceTicketJpa jpa = serviceTicketRepository.findByTicketCode(ticketCode)
            .orElseThrow(() -> new RuntimeException("Service ticket not found: " + ticketCode));
        
        return serviceTicketMapper.toDomain(jpa);
    }

    /**
     * Find service ticket by ticket code (Optional).
     * 
     * @param ticketCode Ticket code (MST_XXXXXX)
     * @return Optional ServiceTicket entity
     */
    @Transactional(readOnly = true)
    public Optional<ServiceTicket> findByTicketCodeOptional(String ticketCode) {
        log.info("Finding service ticket (optional): {}", ticketCode);
        
        return serviceTicketRepository.findByTicketCode(ticketCode)
            .map(serviceTicketMapper::toDomain);
    }

    /**
     * Find service ticket by booking ID.
     * 
     * @param bookingId Booking ID
     * @return ServiceTicket entity
     */
    @Transactional(readOnly = true)
    public ServiceTicket findByBookingId(Integer bookingId) {
        log.info("Finding service ticket by booking: {}", bookingId);
        
        ServiceTicketJpa jpa = serviceTicketRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new RuntimeException("Service ticket not found for booking: " + bookingId));
        
        return serviceTicketMapper.toDomain(jpa);
    }

    /**
     * Update service ticket.
     * 
     * @param ticket ServiceTicket entity to update
     * @return Updated ServiceTicket entity
     */
    @Transactional
    public ServiceTicket updateServiceTicket(ServiceTicket ticket) {
        log.info("Updating service ticket: {}", ticket.getTicketCode());
        
        // Check if ticket exists
        ServiceTicketJpa existing = serviceTicketRepository.findByTicketCode(ticket.getTicketCode())
            .orElseThrow(() -> new RuntimeException("Service ticket not found: " + ticket.getTicketCode()));
        
        // Check immutability
        if (Boolean.TRUE.equals(existing.getImmutable())) {
            throw new RuntimeException("Cannot update immutable service ticket: " + ticket.getTicketCode());
        }
        
        // Convert and save
        ServiceTicketJpa jpa = serviceTicketMapper.toJpa(ticket);
        jpa.setServiceTicketId(existing.getServiceTicketId());  // Preserve ID
        ServiceTicketJpa saved = serviceTicketRepository.save(jpa);
        
        log.info("Updated service ticket: {}", ticket.getTicketCode());
        return serviceTicketMapper.toDomain(saved);
    }

    /**
     * Update service ticket status.
     * 
     * @param ticketCode Ticket code
     * @param newStatus New status
     * @return Updated ServiceTicket entity
     */
    @Transactional
    public ServiceTicket updateStatus(String ticketCode, TicketStatus newStatus) {
        log.info("Updating service ticket status: {} -> {}", ticketCode, newStatus);
        
        ServiceTicket ticket = findByTicketCode(ticketCode);
        ticket.setTicketStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());
        
        return updateServiceTicket(ticket);
    }

    /**
     * Mark service ticket as immutable.
     * 
     * @param ticketCode Ticket code
     * @return Updated ServiceTicket entity
     */
    @Transactional
    public ServiceTicket markAsImmutable(String ticketCode) {
        log.info("Marking service ticket as immutable: {}", ticketCode);
        
        ServiceTicket ticket = findByTicketCode(ticketCode);
        // Không set immutable flag - chỉ dùng status để kiểm soát quyền edit
        ticket.setUpdatedAt(LocalDateTime.now());
        
        return updateServiceTicket(ticket);
    }

    /**
     * Find all service tickets (for admin/reporting).
     * 
     * @return List of all service tickets
     */
    @Transactional(readOnly = true)
    public List<ServiceTicket> findAll() {
        log.info("Finding all service tickets");
        
        List<ServiceTicketJpa> jpaList = serviceTicketRepository.findAll();
        List<ServiceTicket> domainList = new ArrayList<>();
        
        for (ServiceTicketJpa jpa : jpaList) {
            ServiceTicket domain = serviceTicketMapper.toDomain(jpa);
            domainList.add(domain);
        }
        
        return domainList;
    }

    /**
     * Check if service ticket exists by ticket code.
     * 
     * @param ticketCode Ticket code
     * @return true if exists
     */
    @Transactional(readOnly = true)
    public boolean existsByTicketCode(String ticketCode) {
        return serviceTicketRepository.existsByTicketCode(ticketCode);
    }
}
