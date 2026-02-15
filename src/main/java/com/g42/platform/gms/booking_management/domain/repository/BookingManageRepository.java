package com.g42.platform.gms.booking_management.domain.repository;

import com.g42.platform.gms.booking_management.domain.entity.Booking;
import com.g42.platform.gms.booking_management.domain.entity.BookingRequest;
import com.g42.platform.gms.booking_management.domain.entity.TimeSlot;

import java.time.LocalTime;
import java.util.List;

public interface BookingManageRepository {
    List<Booking> getBookedList();

    Booking getBookedDetailById(Integer bookingId);

    List<BookingRequest> getBookingRequestList();

    BookingRequest getBookingRequestById(Integer bookingId);

    TimeSlot getTimeSlotByTime(LocalTime scheduledTime);

    int countReserverdBasedOnTime(LocalTime scheduledTime);
}
