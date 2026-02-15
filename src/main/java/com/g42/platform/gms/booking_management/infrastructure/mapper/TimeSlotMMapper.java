package com.g42.platform.gms.booking_management.infrastructure.mapper;

import com.g42.platform.gms.booking_management.domain.entity.TimeSlot;
import com.g42.platform.gms.booking_management.infrastructure.entity.TimeSlotJpa;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TimeSlotMMapper {
    TimeSlot toDomainTimeSlot(TimeSlotJpa timeSlotJpa);
    List<TimeSlot> toDomainTimeSlotList(List<TimeSlotJpa> timeSlotJpaList);
}
