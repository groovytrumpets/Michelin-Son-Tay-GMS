package com.g42.platform.gms.vehicle.repository;

import com.g42.platform.gms.vehicle.api.internal.VehicleInternalApi;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VehicleInternalApiImpl implements VehicleInternalApi {
    @Autowired
    private VehicleRepository vehicleRepository;

    @Override
    public List<Vehicle> findAllByIds(List<Integer> vehicleIds) {
        return vehicleRepository.findAllById(vehicleIds);
    }
}
