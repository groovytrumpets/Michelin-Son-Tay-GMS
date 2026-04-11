package com.g42.platform.gms.booking_management.api.internal;

import com.g42.platform.gms.booking.customer.domain.entity.Booking;

public interface BookingManageInternalApi {
    Integer findLatestQueueByBookingReservation(Booking booking);
}
