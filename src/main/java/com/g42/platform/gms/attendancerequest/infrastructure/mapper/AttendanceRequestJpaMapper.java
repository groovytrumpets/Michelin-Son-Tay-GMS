package com.g42.platform.gms.attendancerequest.infrastructure.mapper;

import com.g42.platform.gms.attendancerequest.domain.entity.AttendanceRequest;
import com.g42.platform.gms.attendancerequest.infrastructure.entity.AttendanceRequestJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceRequestJpaMapper {

    @Mapping(target = "staffName", ignore = true)
    @Mapping(target = "shiftName", ignore = true)
    AttendanceRequest toDomain(AttendanceRequestJpa jpa);

    AttendanceRequestJpa toJpa(AttendanceRequest domain);
}
