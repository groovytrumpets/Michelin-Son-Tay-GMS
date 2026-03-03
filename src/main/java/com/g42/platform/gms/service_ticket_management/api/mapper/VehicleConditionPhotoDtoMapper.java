package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.service_ticket_management.api.dto.checkin.PhotoUploadResponse;
import com.g42.platform.gms.service_ticket_management.domain.entity.VehicleConditionPhoto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting VehicleConditionPhoto domain entity to PhotoUploadResponse DTO.
 * Follows the same pattern as BookingDtoMapper (Domain → DTO).
 */
@Mapper(componentModel = "spring")
public interface VehicleConditionPhotoDtoMapper {

    @Mapping(target = "message", ignore = true)
    PhotoUploadResponse toResponse(VehicleConditionPhoto domain);
}
