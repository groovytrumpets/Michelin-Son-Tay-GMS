package com.g42.platform.gms.estimation.api.mapper;

import com.g42.platform.gms.estimation.api.dto.ReminderCreateDto;
import com.g42.platform.gms.estimation.api.dto.ReminderRespondDto;
import com.g42.platform.gms.estimation.domain.entity.ServiceReminder;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReminderDtoMapper {
    ServiceReminder toDomain(ReminderCreateDto request);

    ReminderCreateDto toDto(ServiceReminder reminder);

    ReminderRespondDto toResDto(ServiceReminder reminder);
}
