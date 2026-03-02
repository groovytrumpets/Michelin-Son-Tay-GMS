package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA Repository for ServiceTicket entity.
 * 
 * Provides CRUD operations and custom query methods for service tickets.
 */
@Repository
public interface ServiceTicketRepository extends JpaRepository<ServiceTicketJpa, Integer> {
    
    /**
     * Find a service ticket by its unique ticket code.
     * 
     * @param ticketCode the ticket code (format: ST_XXXXXX)
     * @return Optional containing the service ticket if found
     */
    Optional<ServiceTicketJpa> findByTicketCode(String ticketCode);
    
    /**
     * Check if a service ticket exists with the given ticket code.
     * 
     * @param ticketCode the ticket code to check
     * @return true if exists, false otherwise
     */
    boolean existsByTicketCode(String ticketCode);
    
    /**
     * Find a service ticket by booking ID.
     * 
     * @param bookingId the booking ID
     * @return Optional containing the service ticket if found
     */
    Optional<ServiceTicketJpa> findByBookingId(Integer bookingId);
    
    /**
     * Count active (non-cancelled) service tickets for a booking.
     * Production-ready check to prevent duplicate tickets.
     * 
     * @param bookingId the booking ID
     * @return count of active tickets
     */
    @Query("SELECT COUNT(st) FROM ServiceTicketManagement st WHERE st.bookingId = :bookingId AND st.ticketStatus != 'CANCELLED'")
    long countActiveTicketsByBookingId(@Param("bookingId") Integer bookingId);
}
