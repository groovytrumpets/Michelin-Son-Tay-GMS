package com.g42.platform.gms.vehicle.infrastructure.repository;

import com.g42.platform.gms.vehicle.domain.entity.Vehicle;
import com.g42.platform.gms.vehicle.domain.repository.VehicleRepo;
import com.g42.platform.gms.vehicle.infrastructure.mapper.VehicleJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of VehicleRepo domain interface.
 */
@Component
@RequiredArgsConstructor
public class VehicleRepoImpl implements VehicleRepo {

    private final VehicleJpaRepository jpaRepository;
    private final VehicleJpaMapper mapper;

    @Override
    public Optional<Vehicle> findByLicensePlate(String licensePlate) {
        return jpaRepository.findByLicensePlate(licensePlate).map(mapper::toDomain);
    }

    @Override
    public Optional<Vehicle> findById(Integer vehicleId) {
        return jpaRepository.findById(vehicleId).map(mapper::toDomain);
    }

    @Override
    public List<Vehicle> findByCustomerId(Integer customerId) {
        return jpaRepository.findByCustomerId(customerId)
                .stream().map(mapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByLicensePlate(String licensePlate) {
        return jpaRepository.existsByLicensePlate(licensePlate);
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(vehicle)));
    }

    @Override
    public void deleteById(Integer vehicleId) {
        jpaRepository.deleteById(vehicleId);
    }
}
