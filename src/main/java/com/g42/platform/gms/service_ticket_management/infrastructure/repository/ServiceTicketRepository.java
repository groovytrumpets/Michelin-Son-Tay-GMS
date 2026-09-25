package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.repository.query.Param;

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

    // branch_id khai báo updatable = false trên entity nên phải UPDATE thẳng (xem ServiceTicketJpa)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE service_ticket SET branch_id = :branchId WHERE service_ticket_id = :ticketId", nativeQuery = true)
    int updateBranch(@Param("ticketId") Integer ticketId, @Param("branchId") Integer branchId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE booking b JOIN service_ticket t ON t.booking_id = b.booking_id " +
            "SET b.branch_id = :branchId WHERE t.service_ticket_id = :ticketId", nativeQuery = true)
    int updateBookingBranchOfTicket(@Param("ticketId") Integer ticketId, @Param("branchId") Integer branchId);
    
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

    ServiceTicketJpa findFirstByCustomerIdAndVehicleIdAndServiceTicketIdNotOrderByReceivedAtDesc(Integer customerId, Integer vehicleId, Integer serviceTicketId);

    ServiceTicketJpa findServiceTicketJpasByTicketCode(String ticketCode);

    List<ServiceTicketJpa> findAllByCustomerIdAndVehicleId(Integer customerId, Integer vehicleId);

    List<ServiceTicketJpa> findAllByCustomerIdAndVehicleIdOrderByReceivedAtDesc(Integer customerId, Integer vehicleId);

    List<ServiceTicketJpa> findAllByCustomerIdOrderByReceivedAtDesc(Integer customerId);

    Optional<ServiceTicketJpa> findFirstByVehicleIdOrderByCreatedAtDesc(Integer vehicleId);

    List<ServiceTicketJpa> findByTicketStatusAndTicketTypeAndReceivedAtBefore(
            TicketStatus ticketStatus, TicketType ticketType, LocalDateTime receivedAt);

    @Query("select count(st) from ServiceTicketManagement st where st.customerId = :customerId and (st.isDeleted is null or st.isDeleted = false)")
    long countActiveTicketsByCustomerId(@Param("customerId") Integer customerId);

    // ===== Nhập bù phiếu ngày trước (TicketBackfillService) =====

    /**
     * Danh sách phiếu nhập bù cho màn duyệt. Tham số null = không lọc.
     * Lọc theo created_at (lúc bấm nhập) chứ không theo received_at (ngày thực tế).
     */
    @Query("""
        select st from ServiceTicketManagement st
        where st.entryMode = com.g42.platform.gms.service_ticket_management.domain.enums.EntryMode.BACKFILL
          and (:reviewStatus is null or st.backfillReviewStatus = :reviewStatus)
          and (:createdBy is null or st.createdBy = :createdBy)
          and (:createdFrom is null or st.createdAt >= :createdFrom)
          and (:createdTo is null or st.createdAt < :createdTo)
        order by st.createdAt desc
        """)
    org.springframework.data.domain.Page<ServiceTicketJpa> findBackfillTickets(
            @Param("reviewStatus") com.g42.platform.gms.service_ticket_management.domain.enums.BackfillReviewStatus reviewStatus,
            @Param("createdBy") Integer createdBy,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            org.springframework.data.domain.Pageable pageable);

    /** Toàn bộ phiếu nhập bù tạo trong khoảng — để thống kê theo nhân viên. */
    @Query("""
        select st from ServiceTicketManagement st
        where st.entryMode = com.g42.platform.gms.service_ticket_management.domain.enums.EntryMode.BACKFILL
          and (:createdFrom is null or st.createdAt >= :createdFrom)
          and (:createdTo is null or st.createdAt < :createdTo)
        """)
    List<ServiceTicketJpa> findBackfillTicketsCreatedBetween(
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo);

    /** Phiếu chưa huỷ của một khách trong một ngày thực tế — cảnh báo nhập trùng. */
    @Query("""
        select st from ServiceTicketManagement st
        where st.customerId = :customerId
          and st.receivedAt >= :from and st.receivedAt < :to
          and (st.isDeleted is null or st.isDeleted = false)
          and st.ticketStatus <> com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus.CANCELLED
        order by st.receivedAt asc
        """)
    List<ServiceTicketJpa> findActiveByCustomerReceivedBetween(
            @Param("customerId") Integer customerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /** Phiếu nhập thiếu đã gắn vào một phiếu gốc (để đánh số mã phiếu con). */
    long countByBackfillParentTicketId(Integer backfillParentTicketId);

    long countByCustomerIdAndTicketStatus(Integer customerId, com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus ticketStatus);

}
