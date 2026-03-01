package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.service_ticket_management.api.dto.checkin.OdometerResponse;
import com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting OdometerReading domain entity to OdometerResponse DTO.
 * Follows the same pattern as BookingDtoMapper (Domain → DTO).
 */
@Mapper(componentModel = "spring")
public interface OdometerDtoMapper {

    @Mapping(target = "rollbackDetected", ignore = true)
    @Mapping(target = "previousReading", ignore = true)
    @Mapping(target = "warningMessage", ignore = true)
    OdometerResponse toResponse(OdometerReading domain);
}
