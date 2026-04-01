package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for ServiceTicket entity.
 * 
 * Provides CRUD operations and custom query methods for service tickets.
 * Extends JpaSpecificationExecutor để hỗ trợ filter và search với Specification.
 */
@Repository
public interface ServiceTicketRepository extends JpaRepository<ServiceTicketJpa, Integer>, JpaSpecificationExecutor<ServiceTicketJpa> {
    
    /**
     * Find a service ticket by its unique ticket code.
     * 
     * @param ticketCode the ticket code (format: MST_XXXXXX)
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

    ServiceTicketJpa findByServiceTicketId(Integer serviceTicketId);

    List<ServiceTicketJpa> findAllByReceivedAt(LocalDateTime receivedAt);

    @Query("""
    select max(st.queueNumber) from ServiceTicketManagement st where st.receivedAt >=:startOfToday and st.receivedAt <=:endOfToday
        """)
    Integer findMaxQueueNumberForToday(LocalDateTime startOfToday, LocalDateTime endOfToday);

    List<ServiceTicketJpa> findServiceTicketJpasByReceivedAtBetween(LocalDateTime receivedAtAfter, LocalDateTime receivedAtBefore);

    ServiceTicketJpa findFirstByCustomerIdAndServiceTicketIdNot(Integer customerId, Integer serviceTicketId);

    ServiceTicketJpa findFirstByCustomerIdAndServiceTicketIdNotOrderByReceivedAtDesc(Integer customerId, Integer serviceTicketId);
}
