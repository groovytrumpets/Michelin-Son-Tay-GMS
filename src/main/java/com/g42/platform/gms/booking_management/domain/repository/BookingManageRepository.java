package com.g42.platform.gms.booking_management.domain.repository;

import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.domain.entity.*;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.TimeSlotJpa;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingManageRepository {
    Page<Booking> getBookedList(int page, int size, LocalDate date, Boolean isGuest, BookingEnum status);

    Booking getBookedDetailById(Integer bookingId);

    Page<BookingRequest> getBookingRequestList(int page, int size, LocalDate date, Boolean isGuest, BookingRequestStatus status);

    BookingRequest getBookingRequestById(Integer bookingId);

    TimeSlot getTimeSlotByTime(LocalTime scheduledTime);

    int countReserverdBasedOnTime(LocalTime scheduledTime);

    BookingJpa createBookingByRequest(BookingRequest request);

    BookingSlotReservation createBookingSlotReservation(BookingRequest request, BookingJpa bookingId);

    void setConfirmStatus(BookingRequest request);

    void setRequestBooking(BookingRequest request);

    List<CatalogItem> getListOfCatalogById(List<Integer> services);
}
