package com.g42.platform.gms.manager.attendancelocation.infrastructure.mapper;

import com.g42.platform.gms.manager.attendancelocation.domain.entity.AttendanceLocation;
import com.g42.platform.gms.manager.attendancelocation.infrastructure.entity.AttendanceLocationJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttendanceLocationJpaMapper {
    AttendanceLocation toDomain(AttendanceLocationJpa jpa);
    AttendanceLocationJpa toJpa(AttendanceLocation domain);
}
