package com.g42.platform.gms.booking_management.infrastructure.repository;

import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingMRequestJpaRepo extends JpaRepository<BookingRequestJpa, Integer>, JpaSpecificationExecutor<BookingRequestJpa> {
    BookingRequestJpa searchBookingRequestJpaByRequestId(Integer requestId);
}
