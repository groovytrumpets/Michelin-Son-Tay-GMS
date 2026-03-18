package com.g42.platform.gms.booking_management.infrastructure.repository;

import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingRequestJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingManageJpaRepository extends JpaRepository<BookingJpa, Integer>, JpaSpecificationExecutor<BookingJpa> {
    BookingJpa getBookingJpaByBookingId(Integer bookingId);

    BookingJpa getBookingJpaByBookingCode(String bookingCode);
}
