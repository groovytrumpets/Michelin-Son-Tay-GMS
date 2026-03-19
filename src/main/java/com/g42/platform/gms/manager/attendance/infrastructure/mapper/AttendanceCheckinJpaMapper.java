package com.g42.platform.gms.manager.attendance.infrastructure.mapper;

import com.g42.platform.gms.manager.attendance.domain.entity.AttendanceCheckin;
import com.g42.platform.gms.manager.attendance.infrastructure.entity.AttendanceCheckinJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceCheckinJpaMapper {

    @Mapping(target = "staffName", ignore = true)
    @Mapping(target = "shiftName", ignore = true)
    AttendanceCheckin toDomain(AttendanceCheckinJpa jpa);

    @Mapping(target = "shift", ignore = true)
    AttendanceCheckinJpa toJpa(AttendanceCheckin domain);
}
