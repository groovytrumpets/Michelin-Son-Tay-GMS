package com.g42.platform.gms.booking.customer.repository;

import com.g42.platform.gms.booking.customer.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Integer> {
    List<Booking> findByCustomer_CustomerIdOrderByScheduledDateDescScheduledTimeDesc(Integer customerId);
}