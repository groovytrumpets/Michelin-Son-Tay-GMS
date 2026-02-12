package com.g42.platform.gms.booking.customer.infrastructure.mapper;

import com.g42.platform.gms.booking.customer.domain.entity.TimeSlot;
import com.g42.platform.gms.booking.customer.infrastructure.entity.TimeSlotJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TimeSlotMapper {
    TimeSlot toDomain(TimeSlotJpaEntity jpa);
    TimeSlotJpaEntity toJpa(TimeSlot domain);
}
