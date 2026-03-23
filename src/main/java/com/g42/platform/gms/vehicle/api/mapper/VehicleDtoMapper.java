package com.g42.platform.gms.vehicle.api.mapper;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.vehicle.api.dto.VehicleListResponse;
import com.g42.platform.gms.vehicle.api.dto.VehicleResponse;
import com.g42.platform.gms.vehicle.domain.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper: Vehicle domain entity → API response DTOs.
 */
@Mapper(componentModel = "spring")
public interface VehicleDtoMapper {

    @Mapping(target = "customerId", source = "vehicle.customerId")
    @Mapping(target = "customerName", source = "customer.fullName")
    @Mapping(target = "customerPhone", source = "customer.phone")
    @Mapping(target = "lastOdometerReading", ignore = true)
    @Mapping(target = "lastServiceDate", ignore = true)
    VehicleResponse toResponse(Vehicle vehicle, CustomerProfile customer);

    @Mapping(target = "lastOdometerReading", ignore = true)
    @Mapping(target = "lastServiceDate", ignore = true)
    VehicleListResponse.VehicleItem toVehicleItem(Vehicle vehicle);
}
