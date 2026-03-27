package com.g42.platform.gms.manager.attendance.api.mapper;

import com.g42.platform.gms.manager.attendance.api.dto.AttendanceCheckinResponse;
import com.g42.platform.gms.manager.attendance.domain.entity.AttendanceCheckin;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttendanceDtoMapper {
    AttendanceCheckinResponse toResponse(AttendanceCheckin domain);
}
