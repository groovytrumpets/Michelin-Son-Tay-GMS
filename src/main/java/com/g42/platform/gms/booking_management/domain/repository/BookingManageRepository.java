package com.g42.platform.gms.booking_management.domain.repository;

import com.g42.platform.gms.booking_management.api.dto.BookedDetailResponse;
import com.g42.platform.gms.booking_management.domain.entity.Booking;
import com.g42.platform.gms.booking_management.domain.entity.BookingDetail;

import java.util.List;

public interface BookingManageRepository {
    List<Booking> getBookedList();

    Booking getBookedDetailById(Integer bookingId);
}
