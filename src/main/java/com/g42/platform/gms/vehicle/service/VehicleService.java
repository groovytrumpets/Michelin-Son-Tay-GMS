package com.g42.platform.gms.vehicle.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VehicleService {
    @Autowired private VehicleRepository vehicleRepo;

    public Vehicle findOrCreateVehicle(String licensePlate, String brand, String model, CustomerProfile owner) {
        return vehicleRepo.findByLicensePlate(licensePlate)
                .orElseGet(() -> {
                    Vehicle v = new Vehicle();
                    v.setLicensePlate(licensePlate);
                    v.setBrand(brand);
                    v.setModel(model);
                    v.setCustomer(owner); // Gán chủ xe
                    return vehicleRepo.save(v);
                });
    }
}