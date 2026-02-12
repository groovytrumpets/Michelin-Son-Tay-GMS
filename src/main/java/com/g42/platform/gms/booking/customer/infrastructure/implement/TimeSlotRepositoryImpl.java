package com.g42.platform.gms.booking.customer.infrastructure.implement;

import com.g42.platform.gms.booking.customer.domain.entity.TimeSlot;
import com.g42.platform.gms.booking.customer.domain.repository.TimeSlotRepository;
import com.g42.platform.gms.booking.customer.infrastructure.mapper.TimeSlotMapper;
import com.g42.platform.gms.booking.customer.infrastructure.repository.TimeSlotJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TimeSlotRepositoryImpl implements TimeSlotRepository {

    private final TimeSlotJpaRepository jpaRepository;
    private final TimeSlotMapper mapper;

    @Override
    public Optional<TimeSlot> findByStartTime(LocalTime startTime) {
        return jpaRepository.findByStartTime(startTime)
                .map(mapper::toDomain);
    }

    @Override
    public List<TimeSlot> findActiveOrderByStartTime() {
        return jpaRepository.findByIsActiveTrueOrderByStartTimeAsc()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
