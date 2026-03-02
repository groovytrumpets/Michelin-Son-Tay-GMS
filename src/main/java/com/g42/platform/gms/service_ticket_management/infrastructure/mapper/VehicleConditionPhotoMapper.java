package com.g42.platform.gms.service_ticket_management.infrastructure.mapper;

import com.g42.platform.gms.service_ticket_management.domain.entity.VehicleConditionPhoto;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.VehicleConditionPhotoJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleConditionPhotoMapper {

    VehicleConditionPhoto toDomain(VehicleConditionPhotoJpa jpa);

    VehicleConditionPhotoJpa toJpa(VehicleConditionPhoto domain);
}
