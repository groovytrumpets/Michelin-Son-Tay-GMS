package com.g42.platform.gms.vehicle.infrastructure.mapper;

import com.g42.platform.gms.vehicle.domain.entity.Vehicle;
import com.g42.platform.gms.vehicle.infrastructure.entity.VehicleJpa;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper between Vehicle domain entity and VehicleJpa.
 */
@Mapper(componentModel = "spring")
public interface VehicleJpaMapper {

    Vehicle toDomain(VehicleJpa jpa);

    VehicleJpa toJpa(Vehicle domain);
}
