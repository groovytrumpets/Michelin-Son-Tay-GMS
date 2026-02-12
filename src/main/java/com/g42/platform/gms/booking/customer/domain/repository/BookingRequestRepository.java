package com.g42.platform.gms.booking.customer.domain.repository;

import com.g42.platform.gms.booking.customer.domain.entity.BookingRequest;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRequestRepository {
    BookingRequest save(BookingRequest bookingRequest);
    Optional<BookingRequest> findById(Integer requestId);
    Optional<BookingRequest> findByIdAndStatus(Integer requestId, BookingRequestStatus status);
    List<BookingRequest> findByStatus(BookingRequestStatus status);
    List<BookingRequest> findByPhone(String phone);
    List<BookingRequest> findByStatusOrderByCreatedAtDesc(BookingRequestStatus status);
    List<BookingRequest> findExpiredPendingRequests(LocalDateTime now);
    long countByClientIpAndStatusAndCreatedAtAfter(String clientIp, BookingRequestStatus status, LocalDateTime dateTime);
    List<BookingRequest> findRejectedByIpSince(String ip, LocalDateTime since);
}
