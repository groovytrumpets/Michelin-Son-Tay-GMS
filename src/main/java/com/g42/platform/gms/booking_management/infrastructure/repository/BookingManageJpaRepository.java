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

    @Query("""
    SELECT new com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond(
        b.bookingId,
        new com.g42.platform.gms.booking_management.api.dto.CustomerDto(
            c.fullName,
            c.phone,
            null
        ),
        b.scheduledDate,
        b.scheduledTime,
        b.serviceCategory,
        b.status,
        b.description,
        b.isGuest,
        b.createdAt
    )
    FROM BookingJpa b
    LEFT JOIN CustomerProfile c
        ON b.customerId = c.customerId
""")
    Page<BookedRespond> findAllBooked(Specification<BookingJpa> specification, Pageable pageable);
}
