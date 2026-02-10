package com.g42.platform.gms.booking.customer.domain.repository;

import com.g42.platform.gms.booking.customer.domain.entity.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {
    Booking save(Booking booking);
    Optional<Booking> findById(Integer bookingId);
    Optional<Booking> findByIdAndCustomerId(Integer bookingId, Integer customerId);
    List<Booking> findByCustomerIdOrderByDateDesc(Integer customerId);
    void delete(Booking booking);
}
