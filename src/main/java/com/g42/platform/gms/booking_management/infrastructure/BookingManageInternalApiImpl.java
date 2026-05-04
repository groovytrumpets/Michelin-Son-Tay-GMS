package com.g42.platform.gms.booking_management.infrastructure;

import com.g42.platform.gms.booking.customer.domain.entity.Booking;
import com.g42.platform.gms.booking_management.api.internal.BookingManageInternalApi;
import com.g42.platform.gms.booking_management.infrastructure.entity.BookingJpa;
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

    @Override
    public Integer findEstimateId(Integer bookingId) {
        BookingJpa booking = bookingManageJpaRepository.findById(bookingId).orElse(null);
        assert booking != null;
        if (booking.getEstimateId() != null) {
            return booking.getEstimateId();
        }
        return null;
    }
}
