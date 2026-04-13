package com.g42.platform.gms.booking_management.infrastructure.repository;

import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BookingMRequestJpaRepo extends JpaRepository<BookingRequestJpa, Integer>, JpaSpecificationExecutor<BookingRequestJpa> {
    BookingRequestJpa searchBookingRequestJpaByRequestId(Integer requestId);

    BookingRequestJpa searchBookingRequestJpaByRequestCode(String requestCode);

    @Modifying
    @Query("UPDATE BookingRequestJpa b SET b.status = 'EXPIRED' WHERE b.status = 'PENDING' AND b.expiresAt < :now")
    void bulkExpireOldRequests(@Param("now") LocalDateTime now);
}
