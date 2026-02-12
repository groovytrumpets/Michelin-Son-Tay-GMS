package com.g42.platform.gms.booking.customer.infrastructure.repository;

import com.g42.platform.gms.booking.customer.infrastructure.entity.TimeSlotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TimeSlotJpaRepository extends JpaRepository<TimeSlotJpaEntity, Integer> {
    Optional<TimeSlotJpaEntity> findByStartTime(LocalTime startTime);
    
    List<TimeSlotJpaEntity> findByIsActiveTrueOrderByStartTimeAsc();
}
