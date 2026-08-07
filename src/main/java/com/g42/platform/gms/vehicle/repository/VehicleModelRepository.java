package com.g42.platform.gms.vehicle.repository;

import com.g42.platform.gms.vehicle.entity.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Integer> {

    List<VehicleModel> findByActiveTrueOrderByNameAsc();

    List<VehicleModel> findByBrandIdOrderByNameAsc(Integer brandId);

    List<VehicleModel> findByBrandIdAndActiveTrueOrderByNameAsc(Integer brandId);

    Optional<VehicleModel> findByBrandIdAndNameIgnoreCase(Integer brandId, String name);
}
