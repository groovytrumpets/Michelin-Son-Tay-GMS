package com.g42.platform.gms.estimation.infrastructure.mapper;

import com.g42.platform.gms.estimation.domain.entity.ServiceReminder;
import com.g42.platform.gms.estimation.infrastructure.entity.ServiceReminderJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceReminderJpaMapper {
    ServiceReminderJpa toJpa(ServiceReminder domain);

    ServiceReminder toDomain(ServiceReminderJpa save);
}
