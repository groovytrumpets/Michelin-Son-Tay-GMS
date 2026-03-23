package com.g42.platform.gms.vehicle.infrastructure.repository;

import com.g42.platform.gms.vehicle.infrastructure.entity.VehicleJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for VehicleJpa.
 */
@Repository
public interface VehicleJpaRepository extends JpaRepository<VehicleJpa, Integer> {

    Optional<VehicleJpa> findByLicensePlate(String licensePlate);

    List<VehicleJpa> findByCustomerId(Integer customerId);

    boolean existsByLicensePlate(String licensePlate);
}
