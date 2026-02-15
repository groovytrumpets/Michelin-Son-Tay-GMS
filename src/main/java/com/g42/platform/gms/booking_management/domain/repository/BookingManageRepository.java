package com.g42.platform.gms.booking_management.domain.repository;

import com.g42.platform.gms.booking_management.domain.entity.Booking;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequest;
import com.g42.platform.gms.booking_management.domain.entity.BookingSlotReservation;
import com.g42.platform.gms.booking_management.domain.entity.TimeSlot;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.TimeSlotJpa;

import java.time.LocalTime;
import java.util.List;

public interface BookingManageRepository {
    List<Booking> getBookedList();

    Booking getBookedDetailById(Integer bookingId);

    List<BookingRequest> getBookingRequestList();

    BookingRequest getBookingRequestById(Integer bookingId);

    TimeSlot getTimeSlotByTime(LocalTime scheduledTime);

    int countReserverdBasedOnTime(LocalTime scheduledTime);

    BookingJpa createBookingByRequest(BookingRequest request, int customerId);

    BookingSlotReservation createBookingSlotReservation(BookingRequest request, BookingJpa bookingId);

    void setConfirmStatus(BookingRequest request);
}
