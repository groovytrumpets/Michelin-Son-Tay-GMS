package com.g42.platform.gms.dashboard.infrastructure.mapper;

import com.g42.platform.gms.dashboard.domain.entity.StaffNotification;
import com.g42.platform.gms.dashboard.infrastructure.entity.StaffNotificationJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StaffNotifyJpaMapper {
    StaffNotificationJpa toJpa(StaffNotification staffNotification);

    StaffNotification toDomain(StaffNotificationJpa staffNotificationJpa);
}
