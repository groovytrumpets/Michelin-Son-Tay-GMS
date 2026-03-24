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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingManageJpaRepository extends JpaRepository<BookingJpa, Integer>, JpaSpecificationExecutor<BookingJpa> {
    BookingJpa getBookingJpaByBookingId(Integer bookingId);

    BookingJpa getBookingJpaByBookingCode(String bookingCode);
    @Query("""
    select b from BookingJpa b join BookingSlotReservationJpa bs on b.bookingId = bs.booking.bookingId where
        bs.reservedDate=:date
        and bs.startTime=:slot
    """)
    List<BookingJpa> findAllBookingBySlotDate(LocalDate date, LocalTime slot);
}
