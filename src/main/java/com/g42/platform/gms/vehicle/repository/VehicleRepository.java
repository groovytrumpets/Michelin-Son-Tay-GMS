package com.g42.platform.gms.vehicle.repository;

import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.support.PlateKeys;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    /**
     * MỘT xe mang biển số này (xe được đăng ký sớm nhất), không phân biệt cách viết
     * ("30K-86694" = "30k86694").
     *
     * Từ changeset 039 một biển số có thể thuộc nhiều hồ sơ khách (xe dùng chung), nên chỉ
     * dùng hàm này ở chỗ thật sự chỉ cần một xe bất kỳ. Chỗ nào cần biết ai đang giữ biển đó
     * thì dùng {@link #findAllByPlate(String)}; chỗ đã biết khách thì dùng
     * {@link #findByPlateForCustomer(String, Integer)}.
     */
    default Optional<Vehicle> findByLicensePlate(String licensePlate) {
        String key = PlateKeys.normalize(licensePlate);
        return key == null ? Optional.empty() : findFirstByPlateKeyOrderByVehicleIdAsc(key);
    }

    /** Mọi xe mang biển số này, kể cả của các khách khác nhau (xe dùng chung). */
    default List<Vehicle> findAllByPlate(String licensePlate) {
        String key = PlateKeys.normalize(licensePlate);
        return key == null ? List.of() : findByPlateKeyOrderByVehicleIdAsc(key);
    }

    /** Xe mang biển số này CỦA CHÍNH khách đó — dùng khi đã biết khách để không mượn xe người khác. */
    default Optional<Vehicle> findByPlateForCustomer(String licensePlate, Integer customerId) {
        String key = PlateKeys.normalize(licensePlate);
        if (key == null || customerId == null) return Optional.empty();
        return findFirstByPlateKeyAndCustomer_CustomerIdOrderByVehicleIdAsc(key, customerId);
    }

    Optional<Vehicle> findFirstByPlateKeyOrderByVehicleIdAsc(String plateKey);

    Optional<Vehicle> findFirstByPlateKeyAndCustomer_CustomerIdOrderByVehicleIdAsc(String plateKey, Integer customerId);

    List<Vehicle> findByPlateKeyOrderByVehicleIdAsc(String plateKey);

    List<Vehicle> findByPlateKey(String plateKey);

    List<Vehicle> findByCustomer_CustomerId(Integer customerId);
    List<Vehicle> findByCustomer_CustomerIdIn(List<Integer> customerIds);
}
