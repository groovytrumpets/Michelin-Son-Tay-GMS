package com.g42.platform.gms.staff.attendance.api.mapper;

import com.g42.platform.gms.staff.attendance.api.dto.StaffAttendanceRes;
import com.g42.platform.gms.staff.attendance.domain.entity.StaffAttendance;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StaffAttendanceDtoMapper {
    StaffAttendanceRes toDto(StaffAttendance staffAttendance);
}
