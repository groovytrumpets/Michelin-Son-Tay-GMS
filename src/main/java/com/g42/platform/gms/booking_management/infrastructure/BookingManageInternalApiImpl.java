package com.g42.platform.gms.booking_management.infrastructure;

import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking_management.api.internal.BookingManageInternalApi;
import com.g42.platform.gms.booking_management.infrastructure.repository.BookingManageJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingManageInternalApiImpl implements BookingManageInternalApi {
    @Autowired
    private BookingManageJpaRepository bookingManageJpaRepository;

    @Override
    public Integer findLatestQueueByBookingReservation(Booking booking) {

        return bookingManageJpaRepository.findMaxQueueOrderBySLotDate(booking.getScheduledDate(), booking.getScheduledTime());
    }
}
