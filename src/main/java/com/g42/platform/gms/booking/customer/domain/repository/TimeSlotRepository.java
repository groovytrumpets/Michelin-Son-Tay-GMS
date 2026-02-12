package com.g42.platform.gms.booking.customer.domain.repository;

import com.g42.platform.gms.booking.customer.domain.entity.TimeSlot;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TimeSlotRepository {
    Optional<TimeSlot> findByStartTime(LocalTime startTime);
    List<TimeSlot> findActiveOrderByStartTime();
}
