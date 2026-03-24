package com.g42.platform.gms.booking_management.domain.repository;

import com.g42.platform.gms.booking.customer.api.dto.BookingResponse;
import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import com.g42.platform.gms.booking_management.api.dto.confirmed.BookedRespond;
import com.g42.platform.gms.booking_management.api.dto.requesting.ReorderQueueRequest;
import com.g42.platform.gms.booking_management.domain.entity.*;
import com.g42.platform.gms.booking_management.domain.enums.BookingEnum;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
import com.g42.platform.gms.booking_management.infrastructure.entity.TimeSlotJpa;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BookingManageRepository {
    Page<BookedRespond> getBookedList(int page, int size, LocalDate date, Boolean isGuest, BookingEnum status, String search);

    Booking getBookedDetailById(String bookingId);

    Page<BookingRequest> getBookingRequestList(int page, int size, LocalDate date, Boolean isGuest, BookingRequestStatus status, String search);

    BookingRequest getBookingRequestById(String bookingCode);

    TimeSlot getTimeSlotByTime(LocalTime scheduledTime);

    int countReserverdBasedOnTime(LocalTime scheduledTime);

    BookingJpa createBookingByRequest(BookingRequest request, int customerId);

    BookingSlotReservation createBookingSlotReservation(BookingRequest request, BookingJpa bookingId);

    void setConfirmStatus(BookingRequest request);

    void setRequestBooking(BookingRequest request);

    List<CatalogItem> getListOfCatalogById(List<Integer> services);

    Boolean reorderQueue(ReorderQueueRequest request);

    List<Booking> getBookingBySlot(LocalDate date, LocalTime slot);

    Booking getBookedById(Integer bookingId);

    Booking save(Booking booking);
}
