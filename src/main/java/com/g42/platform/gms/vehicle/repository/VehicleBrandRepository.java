package com.g42.platform.gms.vehicle.repository;

import com.g42.platform.gms.vehicle.entity.VehicleBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Integer> {

    List<VehicleBrand> findAllByOrderBySortOrderAscNameAsc();

    List<VehicleBrand> findByActiveTrueOrderBySortOrderAscNameAsc();

    Optional<VehicleBrand> findByNameIgnoreCase(String name);
}
