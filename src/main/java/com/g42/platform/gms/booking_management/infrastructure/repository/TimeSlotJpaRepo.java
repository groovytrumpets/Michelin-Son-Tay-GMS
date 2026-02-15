package com.g42.platform.gms.booking_management.infrastructure.repository;

import com.g42.platform.gms.booking_management.domain.entity.TimeSlot;
import com.g42.platform.gms.booking_management.infrastructure.entity.TimeSlotJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;

public interface TimeSlotJpaRepo extends JpaRepository<TimeSlotJpa,Integer> {
    TimeSlotJpa getTimeSlotsByStartTime(LocalTime startTime);
}
