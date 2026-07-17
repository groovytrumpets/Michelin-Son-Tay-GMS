package com.g42.platform.gms.manager.attendancelocation.api.mapper;

import com.g42.platform.gms.manager.attendancelocation.api.dto.AttendanceLocationResponse;
import com.g42.platform.gms.manager.attendancelocation.domain.entity.AttendanceLocation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttendanceLocationDtoMapper {
    AttendanceLocationResponse toResponse(AttendanceLocation domain);
}
