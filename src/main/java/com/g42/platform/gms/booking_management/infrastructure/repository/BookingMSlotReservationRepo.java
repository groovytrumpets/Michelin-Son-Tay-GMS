package com.g42.platform.gms.booking_management.infrastructure.repository;

import com.g42.platform.gms.booking_management.domain.entity.TimeSlot;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingSlotReservationJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
@Repository
public interface BookingMSlotReservationRepo extends JpaRepository<BookingSlotReservationJpa,Integer> {
    List<BookingSlotReservationJpa> findAllByStartTime(LocalTime startTime);
}
