package com.g42.platform.gms.vehicle.domain.repository;

import com.g42.platform.gms.vehicle.domain.entity.Vehicle;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for Vehicle.
 * Application services depend on this interface, not on JPA directly.
 */
public interface VehicleRepo {

    Optional<Vehicle> findByLicensePlate(String licensePlate);

    Optional<Vehicle> findById(Integer vehicleId);

    List<Vehicle> findByCustomerId(Integer customerId);

    boolean existsByLicensePlate(String licensePlate);

    Vehicle save(Vehicle vehicle);

    void deleteById(Integer vehicleId);
}
