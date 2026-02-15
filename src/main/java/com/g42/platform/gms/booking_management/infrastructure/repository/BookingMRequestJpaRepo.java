package com.g42.platform.gms.booking_management.infrastructure.repository;

import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingMRequestJpaRepo extends JpaRepository<BookingRequestJpa, Integer> {
    BookingRequestJpa searchBookingRequestJpaByRequestId(Integer requestId);
}
