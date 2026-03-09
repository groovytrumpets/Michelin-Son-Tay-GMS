package com.g42.platform.gms.service_ticket_management.api.mapper;

import com.g42.platform.gms.service_ticket_management.api.dto.checkin.CreateVehicleResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.checkin.CustomerVehiclesResponse;
import com.g42.platform.gms.service_ticket_management.api.dto.checkin.VehicleResponse;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper cho Vehicle responses trong quy trình Check-in.
 */
@Mapper(componentModel = "spring")
public interface VehicleMapper {
    
    /**
     * Map Vehicle sang CreateVehicleResponse.
     */
    @Mapping(target = "vehicleId", source = "vehicleId")
    @Mapping(target = "licensePlate", source = "licensePlate")
    @Mapping(target = "make", source = "brand")
    @Mapping(target = "model", source = "model")
    @Mapping(target = "year", source = "manufactureYear")
    @Mapping(target = "customerId", source = "customer.customerId")
    @Mapping(target = "message", constant = "Tạo xe mới thành công")
    CreateVehicleResponse toCreateResponse(Vehicle vehicle);
    
    /**
     * Map Vehicle sang VehicleResponse.
     */
    @Mapping(target = "vehicleId", source = "vehicleId")
    @Mapping(target = "licensePlate", source = "licensePlate")
    @Mapping(target = "make", source = "brand")
    @Mapping(target = "model", source = "model")
    @Mapping(target = "year", source = "manufactureYear")
    @Mapping(target = "customerId", source = "customer.customerId")
    @Mapping(target = "isNewVehicle", ignore = true)
    @Mapping(target = "ticketCode", ignore = true)
    VehicleResponse toVehicleResponse(Vehicle vehicle);
    
    /**
     * Map Vehicle sang VehicleInfo (cho customer vehicles list).
     */
    @Mapping(target = "vehicleId", source = "vehicleId")
    @Mapping(target = "licensePlate", source = "licensePlate")
    @Mapping(target = "make", source = "brand")
    @Mapping(target = "model", source = "model")
    @Mapping(target = "year", source = "manufactureYear")
    @Mapping(target = "lastOdometerReading", ignore = true)
    @Mapping(target = "lastServiceDate", ignore = true)
    CustomerVehiclesResponse.VehicleInfo toVehicleInfo(Vehicle vehicle);
    
    /**
     * Map list of Vehicles sang list of VehicleInfo.
     */
    List<CustomerVehiclesResponse.VehicleInfo> toVehicleInfoList(List<Vehicle> vehicles);
}
