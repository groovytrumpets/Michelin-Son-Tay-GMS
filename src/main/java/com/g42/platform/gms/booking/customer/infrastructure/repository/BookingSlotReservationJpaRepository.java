package com.g42.platform.gms.booking.customer.infrastructure.repository;

import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingSlotReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingSlotReservationJpaRepository extends JpaRepository<BookingSlotReservationJpaEntity, Integer> {
    List<BookingSlotReservationJpaEntity> findByReservedDateAndStartTime(LocalDate date, LocalTime time);
    
    List<BookingSlotReservationJpaEntity> findByBooking_BookingId(Integer bookingId);
    
    void deleteByBooking_BookingId(Integer bookingId);
}
