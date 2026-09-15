package com.g42.platform.gms.vehicle.repository;

import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.support.PlateKeys;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    /**
     * Tra xe theo biển số, KHÔNG phân biệt cách viết ("30K-86694" = "30k86694").
     * So qua cột plate_key để mọi chỗ kiểm tra trùng biển số dùng cùng một quy tắc.
     */
    default Optional<Vehicle> findByLicensePlate(String licensePlate) {
        String key = PlateKeys.normalize(licensePlate);
        return key == null ? Optional.empty() : findFirstByPlateKeyOrderByVehicleIdAsc(key);
    }

    Optional<Vehicle> findFirstByPlateKeyOrderByVehicleIdAsc(String plateKey);

    List<Vehicle> findByPlateKey(String plateKey);

    List<Vehicle> findByCustomer_CustomerId(Integer customerId);
    List<Vehicle> findByCustomer_CustomerIdIn(List<Integer> customerIds);
}
