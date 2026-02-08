package com.g42.platform.gms.booking.customer.repository;

import com.g42.platform.gms.booking.customer.entity.BookingRequest;
import com.g42.platform.gms.booking.customer.entity.BookingRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRequestRepository extends JpaRepository<BookingRequest, Integer> {
    
    List<BookingRequest> findByStatus(BookingRequestStatus status);
    
    List<BookingRequest> findByPhone(String phone);
    
    List<BookingRequest> findByStatusOrderByCreatedAtDesc(BookingRequestStatus status);
    
    @Query("SELECT br FROM BookingRequest br WHERE br.status = 'PENDING' AND br.expiresAt < :now")
    List<BookingRequest> findExpiredPendingRequests(LocalDateTime now);
    
    Optional<BookingRequest> findByRequestIdAndStatus(Integer requestId, BookingRequestStatus status);
    
    long countByClientIpAndStatusAndCreatedAtAfter(String clientIp, BookingRequestStatus status, LocalDateTime dateTime);
    
    @Query("SELECT br FROM BookingRequest br WHERE br.clientIp = :ip AND br.status = 'REJECTED' AND br.createdAt >= :since")
    List<BookingRequest> findRejectedByIpSince(@Param("ip") String ip, @Param("since") LocalDateTime since);
}
