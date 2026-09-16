package com.g42.platform.gms.vehicle.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.OdometerHistoryJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.OdometerHistoryRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.vehicle.dto.PlateOwnerDto;
import com.g42.platform.gms.vehicle.dto.VehicleCreateRequest;
import com.g42.platform.gms.vehicle.dto.VehicleListResponse;
import com.g42.platform.gms.vehicle.dto.VehicleUpdateRequest;
import com.g42.platform.gms.vehicle.dto.VehicleUpdateResponse;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    /** {bảng, nhãn hiển thị} — các bảng lưu vehicle_id, nhiều bảng không có khoá ngoại. */
    private static final String[][] VEHICLE_REFERENCES = {
        {"service_ticket", "phiếu dịch vụ"},
        {"booking", "lịch hẹn"},
        {"odometer_history", "lần ghi số km"},
        {"legacy_visit", "lượt sổ cũ"},
        {"service_reminder", "nhắc lịch bảo dưỡng"},
        {"part_warranty", "bảo hành phụ tùng"},
        {"vehicle_specification", "thông số kỹ thuật"},
    };

    private final VehicleRepository vehicleRepo;
    private final CustomerProfileRepository customerRepository;
    private final OdometerHistoryRepository odometerRepository;
    private final ServiceTicketRepository serviceTicketRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Xe mang biển số này CỦA CHÍNH khách đó, chưa có thì tạo mới cho khách.
     * Không "mượn" xe cùng biển của khách khác: từ changeset 039 một biển số dùng chung được
     * cho nhiều hồ sơ, mỗi hồ sơ giữ lịch sử của riêng mình.
     */
    public Vehicle findOrCreateVehicle(String licensePlate, String brand, String model, CustomerProfile owner) {
        return vehicleRepo
                .findByPlateForCustomer(licensePlate, owner == null ? null : owner.getCustomerId())
                .orElseGet(() -> {
                    Vehicle v = new Vehicle();
                    v.setLicensePlate(licensePlate);
                    v.setBrand(brand);
                    v.setModel(model);
                    v.setCustomer(owner); // Gán chủ xe
                    return vehicleRepo.save(v);
                });
    }

    /**
     * Get all vehicles owned by a customer.
     * Includes last odometer reading and last service date for each vehicle.
     *
     * @param customerId Customer ID
     * @return VehicleListResponse with list of vehicles
     */
    @Transactional(readOnly = true)
    public VehicleListResponse getCustomerVehicles(Integer customerId) {
        log.info("Getting vehicles for customer: {}", customerId);

        // === 1. Validate customer exists ===
        CustomerProfile customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        // === 2. Get all vehicles ===
        List<Vehicle> vehicles = vehicleRepo.findByCustomer_CustomerId(customerId);

        // === 3. Map to response ===
        VehicleListResponse response = new VehicleListResponse();
        response.setCustomerId(customerId);
        response.setCustomerName(customer.getFullName());
        response.setCustomerPhone(customer.getPhone());

        List<VehicleListResponse.VehicleInfo> vehicleInfos = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            VehicleListResponse.VehicleInfo info = new VehicleListResponse.VehicleInfo();
            info.setVehicleId(vehicle.getVehicleId());
            info.setLicensePlate(vehicle.getLicensePlate());
            info.setMake(vehicle.getBrand());
            info.setModel(vehicle.getModel());
            info.setYear(vehicle.getManufactureYear());

            // Get last odometer reading
            Optional<OdometerHistoryJpa> lastReading = odometerRepository.findLatestByVehicleId(vehicle.getVehicleId());
            if (lastReading.isPresent()) {
                info.setLastOdometerReading(lastReading.get().getReading());
            }

            // Get last service date (trước đây findAll() toàn bảng service_ticket cho mỗi xe)
            serviceTicketRepository.findFirstByVehicleIdOrderByCreatedAtDesc(vehicle.getVehicleId())
                .filter(ticket -> ticket.getCreatedAt() != null)
                .ifPresent(ticket -> info.setLastServiceDate(ticket.getCreatedAt().toLocalDate()));

            vehicleInfos.add(info);
        }

        response.setVehicles(vehicleInfos);

        log.info("Found {} vehicles for customer: {}", vehicleInfos.size(), customerId);
        return response;
    }

    /**
     * Add a vehicle to a customer (staff screen /vehicle-management).
     * Biển số chuẩn hoá trim + in hoa giống {@link #updateVehicle}.
     */
    @Transactional
    public VehicleUpdateResponse createVehicle(VehicleCreateRequest request) {
        CustomerProfile customer = customerRepository.findById(request.getCustomerId())
            .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        String plate = request.getLicensePlate().trim().toUpperCase();
        checkPlateUsage(plate, request.getCustomerId(), null, Boolean.TRUE.equals(request.getAllowSharedPlate()));

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(plate);
        vehicle.setBrand(trimToNull(request.getBrand()));
        vehicle.setModel(trimToNull(request.getModel()));
        vehicle.setManufactureYear(request.getManufactureYear());
        vehicle.setCustomer(customer);
        return toUpdateResponse(vehicleRepo.save(vehicle));
    }

    /**
     * Update an existing vehicle's plate/brand/model/year.
     * Used by staff when editing a customer's profile in the admin panel.
     *
     * @param vehicleId ID of the vehicle to update
     * @param request New vehicle info
     * @return Updated vehicle entity
     */
    @Transactional
    public VehicleUpdateResponse updateVehicle(Integer vehicleId, VehicleUpdateRequest request) {
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy xe"));

        String newPlate = request.getLicensePlate().trim().toUpperCase();
        Integer ownerId = vehicle.getCustomer() == null ? null : vehicle.getCustomer().getCustomerId();
        checkPlateUsage(newPlate, ownerId, vehicleId, Boolean.TRUE.equals(request.getAllowSharedPlate()));

        vehicle.setLicensePlate(newPlate);
        vehicle.setBrand(request.getBrand() != null ? request.getBrand().trim() : null);
        vehicle.setModel(request.getModel() != null ? request.getModel().trim() : null);
        vehicle.setManufactureYear(request.getManufactureYear());
        return toUpdateResponse(vehicleRepo.save(vehicle));
    }

    /**
     * Xoá xe. Chỉ cho xoá xe chưa phát sinh dữ liệu (phiếu dịch vụ, lịch hẹn, sổ cũ...) —
     * nhiều bảng lưu vehicle_id mà không có khoá ngoại nên phải tự đếm, xoá bừa sẽ để lại
     * lịch sử mồ côi. Xe nhập sai biển số thì sửa biển số thay vì xoá.
     */
    @Transactional
    public void deleteVehicle(Integer vehicleId) {
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy xe"));

        List<String> usages = new ArrayList<>();
        for (String[] ref : VEHICLE_REFERENCES) {
            long count = countReferences(ref[0], vehicleId);
            if (count > 0) usages.add(count + " " + ref[1]);
        }
        if (!usages.isEmpty()) {
            throw new RuntimeException("Không thể xoá xe " + vehicle.getLicensePlate()
                + " vì đã có " + String.join(", ", usages)
                + ". Nếu nhập sai biển số, hãy dùng chức năng Sửa.");
        }

        vehicleRepo.delete(vehicle);
        vehicleRepo.flush();
    }

    private long countReferences(String table, Integer vehicleId) {
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE vehicle_id = ?", Long.class, vehicleId);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            // Bảng chưa có trên DB này (VD: bảng thêm ở changeset 010) — bỏ qua.
            log.warn("Skip vehicle reference check on {}: {}", table, e.getMessage());
            return 0;
        }
    }

    /**
     * Mọi hồ sơ khách đang gắn biển số này (không phân biệt cách viết biển số).
     * Dùng cho màn tra cứu theo biển số: xe dùng chung thì lễ tân phải thấy đủ để chọn đúng người.
     */
    @Transactional(readOnly = true)
    public List<PlateOwnerDto> findOwnersByPlate(String licensePlate) {
        return vehicleRepo.findAllByPlate(licensePlate).stream().map(VehicleService::toPlateOwner).toList();
    }

    /**
     * Quy tắc trùng biển số (changeset 039):
     * - Cùng một khách có hai xe cùng biển số = nhập trùng → chặn hẳn.
     * - Khác khách = xe dùng chung (vợ chồng, gia đình, công ty) → cho phép, nhưng lần đầu trả
     *   {@link SharedPlateException} kèm danh sách chủ hiện tại để nhân viên xác nhận, tránh
     *   trường hợp thật ra chỉ là gõ nhầm biển số.
     */
    private void checkPlateUsage(String plate, Integer customerId, Integer selfVehicleId, boolean allowShared) {
        List<Vehicle> existing = vehicleRepo.findAllByPlate(plate).stream()
                .filter(v -> selfVehicleId == null || !selfVehicleId.equals(v.getVehicleId()))
                .toList();
        if (existing.isEmpty()) return;

        boolean sameCustomer = customerId != null && existing.stream()
                .anyMatch(v -> v.getCustomer() != null && customerId.equals(v.getCustomer().getCustomerId()));
        if (sameCustomer) {
            throw new RuntimeException("Khách hàng này đã có xe biển số " + plate + " trong hệ thống.");
        }
        if (allowShared) return;

        List<PlateOwnerDto> owners = existing.stream().map(VehicleService::toPlateOwner).toList();
        String names = owners.stream()
                .map(o -> o.getCustomerName() == null || o.getCustomerName().isBlank()
                        ? "khách #" + o.getCustomerId() : o.getCustomerName())
                .collect(java.util.stream.Collectors.joining(", "));
        throw new SharedPlateException("Biển số " + plate + " đang thuộc về " + names
                + ". Nếu là xe dùng chung thì xác nhận để thêm vào hồ sơ này, còn không hãy kiểm tra lại biển số.",
                owners);
    }

    private static PlateOwnerDto toPlateOwner(Vehicle vehicle) {
        CustomerProfile owner = vehicle.getCustomer();
        return new PlateOwnerDto(
                vehicle.getVehicleId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getManufactureYear(),
                owner == null ? null : owner.getCustomerId(),
                owner == null ? null : owner.getFullName(),
                owner == null ? null : owner.getPhone(),
                null);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static VehicleUpdateResponse toUpdateResponse(Vehicle vehicle) {
        VehicleUpdateResponse response = new VehicleUpdateResponse();
        response.setVehicleId(vehicle.getVehicleId());
        response.setLicensePlate(vehicle.getLicensePlate());
        response.setBrand(vehicle.getBrand());
        response.setModel(vehicle.getModel());
        response.setManufactureYear(vehicle.getManufactureYear());
        return response;
    }
}
