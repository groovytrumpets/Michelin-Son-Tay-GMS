package com.g42.platform.gms.manager.schedule.infrastructure.mapper;

import com.g42.platform.gms.manager.schedule.domain.entity.WorkShift;
import com.g42.platform.gms.manager.schedule.infrastructure.entity.WorkShiftJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkShiftJpaMapper {
    WorkShift toDomain(WorkShiftJpa jpa);
    WorkShiftJpa toJpa(WorkShift domain);
}
