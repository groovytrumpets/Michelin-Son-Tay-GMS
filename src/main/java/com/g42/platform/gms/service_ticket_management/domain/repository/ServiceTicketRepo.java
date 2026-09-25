package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
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

    /** @param walkIn null = không lọc; true/false = chỉ phiếu bán lẻ khách vãng lai / phiếu khách có hồ sơ */
    Page<ServiceTicket> findAll(TicketStatus status, LocalDate date, String search, TicketType ticketType,
                                Boolean walkIn, Integer branchId, Pageable pageable);

    /** Đổi xưởng của phiếu (và lịch hẹn gốc nếu có) — cột branch_id không cập nhật qua save(). */
    void updateBranch(Integer serviceTicketId, Integer branchId);

    Page<ServiceTicket> findByAssignedStaff(Integer staffId, TicketStatus status, LocalDate date, String search, Pageable pageable);

    Page<ServiceTicket> findByTechnicianCompleted(Integer technicianId, LocalDate startDate, LocalDate endDate, String licensePlate, Pageable pageable);

    List<ServiceTicket> findAllByDate(LocalDateTime receivedAt);

    Integer findMaxQueueNumberForToday(LocalDateTime startOfToday, LocalDateTime endOfToday);

    List<ServiceTicket> findBetween(LocalDateTime start, LocalDateTime end);

    ServiceTicket findPerviousCustomerService(Integer customerId, Integer serviceTicketId,Integer vehicleId);

    List<ServiceTicket> findByCustomerAndVehicle(Integer customerId, Integer vehicleId);

    List<ServiceTicket> findByCustomerId(Integer customerId);

    /**
     * Phieu dang giu hang (HOLDING) tao truoc moc thoi gian - job quet qua han dung.
     */
    List<ServiceTicket> findExpiredHoldingTickets(TicketType ticketType, LocalDateTime createdBefore);
}
