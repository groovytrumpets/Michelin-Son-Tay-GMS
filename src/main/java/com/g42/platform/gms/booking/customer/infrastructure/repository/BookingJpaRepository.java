package com.g42.platform.gms.booking.customer.infrastructure.repository;

import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface BookingJpaRepository extends JpaRepository<BookingJpaEntity, Integer> {
    List<BookingJpaEntity> findByCustomer_CustomerIdOrderByScheduledDateDescScheduledTimeDesc(Integer customerId);

    Optional<BookingJpaEntity> findByBookingCode(String bookingCode);
    
    boolean existsByBookingCode(String bookingCode);

    long countByScheduledDateAndScheduledTimeAndStatusIn(
            LocalDate scheduledDate,
            LocalTime scheduledTime,
            List<BookingStatus> statuses
    );
}
