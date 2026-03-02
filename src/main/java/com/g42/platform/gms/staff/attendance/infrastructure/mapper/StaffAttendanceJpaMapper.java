package com.g42.platform.gms.staff.attendance.infrastructure.mapper;

import com.g42.platform.gms.staff.attendance.domain.entity.StaffAttendance;
import com.g42.platform.gms.staff.attendance.infrastructure.entity.StaffAttendanceJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StaffAttendanceJpaMapper {
    StaffAttendance toDomain (StaffAttendanceJpa staffAttendanceJpa);
}
