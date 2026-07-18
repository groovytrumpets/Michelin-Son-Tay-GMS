package com.g42.platform.gms.attendancerequest.api.mapper;

import com.g42.platform.gms.attendancerequest.api.dto.AttendanceRequestResponse;
import com.g42.platform.gms.attendancerequest.domain.entity.AttendanceRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AttendanceRequestDtoMapper {
    AttendanceRequestResponse toResponse(AttendanceRequest domain);
}
