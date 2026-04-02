package com.g42.platform.gms.vehicle.api.internal;

import com.g42.platform.gms.vehicle.entity.Vehicle;

import java.util.List;

public interface VehicleInternalApi {
    List<Vehicle> findAllByIds(List<Integer> vehicleIds);
}
