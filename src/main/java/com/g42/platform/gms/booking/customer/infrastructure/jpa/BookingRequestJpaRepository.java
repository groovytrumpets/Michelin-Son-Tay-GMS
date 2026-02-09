package com.g42.platform.gms.booking.customer.infrastructure.jpa;

import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingRequestJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRequestJpaRepository extends JpaRepository<BookingRequestJpaEntity, Integer> {
    
    List<BookingRequestJpaEntity> findByStatus(BookingRequestStatus status);
    
    List<BookingRequestJpaEntity> findByPhone(String phone);
    
    List<BookingRequestJpaEntity> findByStatusOrderByCreatedAtDesc(BookingRequestStatus status);
    
    @Query("SELECT br FROM BookingRequestJpaEntity br WHERE br.status = 'PENDING' AND br.expiresAt < :now")
    List<BookingRequestJpaEntity> findExpiredPendingRequests(LocalDateTime now);
    
    Optional<BookingRequestJpaEntity> findByRequestIdAndStatus(Integer requestId, BookingRequestStatus status);
    
    long countByClientIpAndStatusAndCreatedAtAfter(String clientIp, BookingRequestStatus status, LocalDateTime dateTime);
    
    @Query("SELECT br FROM BookingRequestJpaEntity br WHERE br.clientIp = :ip AND br.status = 'REJECTED' AND br.createdAt >= :since")
    List<BookingRequestJpaEntity> findRejectedByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);
}
