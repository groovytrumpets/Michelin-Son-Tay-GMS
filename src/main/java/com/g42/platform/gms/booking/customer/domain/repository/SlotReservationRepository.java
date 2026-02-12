package com.g42.platform.gms.booking.customer.domain.repository;

import com.g42.platform.gms.booking.customer.domain.entity.SlotReservation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SlotReservationRepository {
    List<SlotReservation> findByDateAndTime(LocalDate date, LocalTime time);
    List<SlotReservation> findByBookingId(Integer bookingId);
    void deleteByBookingId(Integer bookingId);
    SlotReservation save(SlotReservation reservation);
}
