package com.g42.platform.gms.service_ticket_management.infrastructure.mapper;

import com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.OdometerHistoryJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OdometerReadingMapper {

    OdometerReading toDomain(OdometerHistoryJpa jpa);

    OdometerHistoryJpa toJpa(OdometerReading domain);
}
