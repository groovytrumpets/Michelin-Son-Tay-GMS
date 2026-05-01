package com.g42.platform.gms.dashboard.api.mapper;

import com.g42.platform.gms.dashboard.api.dto.NotificationCreateDto;
import com.g42.platform.gms.dashboard.api.dto.NotificationRespondDto;
import com.g42.platform.gms.dashboard.domain.entity.StaffNotification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StaffNotifyDtoMapper {

    StaffNotification toDomain(NotificationCreateDto dto);

    NotificationRespondDto toDto(StaffNotification staffNotification);
}
