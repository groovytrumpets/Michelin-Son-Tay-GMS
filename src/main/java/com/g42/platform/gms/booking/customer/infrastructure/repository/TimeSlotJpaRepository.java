package com.g42.platform.gms.booking.customer.infrastructure.repository;

import com.g42.platform.gms.booking.customer.infrastructure.entity.TimeSlotJpaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TimeSlotJpaRepository extends JpaRepository<TimeSlotJpaEntity, Integer> {
    Optional<TimeSlotJpaEntity> findByStartTime(LocalTime startTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TimeSlotJpaEntity t WHERE t.startTime = :startTime")
    Optional<TimeSlotJpaEntity> findByStartTimeWithLock(@Param("startTime") LocalTime startTime);

    List<TimeSlotJpaEntity> findByIsActiveTrueOrderByStartTimeAsc();
    
    List<TimeSlotJpaEntity> findAllByOrderByStartTimeAsc();
}
