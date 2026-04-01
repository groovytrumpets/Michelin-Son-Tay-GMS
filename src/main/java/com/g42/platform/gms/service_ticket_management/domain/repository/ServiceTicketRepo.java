package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for ServiceTicket.
 * Application services depend on this interface, not on JPA repository directly.
 */
public interface ServiceTicketRepo {

    Optional<ServiceTicket> findByTicketCode(String ticketCode);

    Optional<ServiceTicket> findByBookingId(Integer bookingId);

    ServiceTicket findByServiceTicketId(Integer serviceTicketId);

    boolean existsByTicketCode(String ticketCode);

    ServiceTicket save(ServiceTicket ticket);

    void deleteById(Integer id);

    List<ServiceTicket> findAll();

    Page<ServiceTicket> findAll(TicketStatus status, LocalDate date, String search, Pageable pageable);

    Page<ServiceTicket> findByAssignedStaff(Integer staffId, TicketStatus status, LocalDate date, String search, Pageable pageable);

    Page<ServiceTicket> findByTechnicianCompleted(Integer technicianId, LocalDate startDate, LocalDate endDate, String licensePlate, Pageable pageable);

    List<ServiceTicket> findAllByDate(LocalDateTime receivedAt);

    Integer findMaxQueueNumberForToday(LocalDateTime startOfToday, LocalDateTime endOfToday);

    List<ServiceTicket> findBetween(LocalDateTime start, LocalDateTime end);
}
