package com.g42.platform.gms.vehicle.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.service_ticket_management.domain.repository.OdometerReadingRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.vehicle.api.dto.CreateVehicleRequest;
import com.g42.platform.gms.vehicle.api.dto.UpdateVehicleRequest;
import com.g42.platform.gms.vehicle.api.dto.VehicleListResponse;
import com.g42.platform.gms.vehicle.api.dto.VehicleResponse;
import com.g42.platform.gms.vehicle.api.mapper.VehicleDtoMapper;
import com.g42.platform.gms.vehicle.domain.entity.Vehicle;
import com.g42.platform.gms.vehicle.domain.repository.VehicleRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Application service for Vehicle management.
 * Follows the same convention as ServiceTicketManageService:
 * - Own domain: accessed via domain repository interface (VehicleRepo)
 * - Other modules: inject their repositories directly (same pattern as STM)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleManageService {

    private final VehicleRepo vehicleRepo;
    private final CustomerProfileRepository customerRepository;
    private final OdometerReadingRepo odometerRepo;
    private final ServiceTicketRepository serviceTicketRepository;
    private final VehicleDtoMapper dtoMapper;

    // ===== Internal: find or create (used by CheckInService) =====

    @Transactional
    public Vehicle findOrCreate(String licensePlate, String brand, String model, Integer customerId) {
        return vehicleRepo.findByLicensePlate(licensePlate).orElseGet(() -> {
            Vehicle v = new Vehicle();
            v.setLicensePlate(licensePlate);
            v.setBrand(brand);
            v.setModel(model);
            v.setCustomerId(customerId);
            return vehicleRepo.save(v);
        });
    }

    // ===== GET list by customer =====

    @Transactional(readOnly = true)
    public VehicleListResponse getCustomerVehicles(Integer customerId) {
        log.info("Getting vehicles for customer: {}", customerId);

        CustomerProfile customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + customerId));

        List<Vehicle> vehicles = vehicleRepo.findByCustomerId(customerId);

        VehicleListResponse response = new VehicleListResponse();
        response.setCustomerId(customerId);
        response.setCustomerName(customer.getFullName());
        response.setCustomerPhone(customer.getPhone());
        response.setVehicles(vehicles.stream()
                .map(v -> buildVehicleItem(v))
                .collect(Collectors.toList()));

        log.info("Found {} vehicles for customer: {}", vehicles.size(), customerId);
        return response;
    }

    // ===== GET single vehicle =====

    @Transactional(readOnly = true)
    public VehicleResponse getVehicleDetail(Integer vehicleId) {
        log.info("Getting vehicle detail: {}", vehicleId);

        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe: " + vehicleId));

        CustomerProfile customer = customerRepository.findById(vehicle.getCustomerId()).orElse(null);
        return buildVehicleResponse(vehicle, customer);
    }

    // ===== CREATE =====

    @Transactional
    public VehicleResponse createVehicle(CreateVehicleRequest request) {
        log.info("Creating vehicle: {}", request.getLicensePlate());

        if (vehicleRepo.existsByLicensePlate(request.getLicensePlate())) {
            throw new RuntimeException("Biển số xe đã tồn tại: " + request.getLicensePlate());
        }

        CustomerProfile customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + request.getCustomerId()));

        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setBrand(request.getBrand());
        vehicle.setModel(request.getModel());
        vehicle.setManufactureYear(request.getManufactureYear());
        vehicle.setCustomerId(request.getCustomerId());

        Vehicle saved = vehicleRepo.save(vehicle);
        log.info("Vehicle created: id={}", saved.getVehicleId());
        return buildVehicleResponse(saved, customer);
    }

    // ===== UPDATE =====

    @Transactional
    public VehicleResponse updateVehicle(Integer vehicleId, UpdateVehicleRequest request) {
        log.info("Updating vehicle: {}", vehicleId);

        Vehicle vehicle = vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe: " + vehicleId));

        if (request.getBrand() != null) vehicle.setBrand(request.getBrand());
        if (request.getModel() != null) vehicle.setModel(request.getModel());
        if (request.getManufactureYear() != null) vehicle.setManufactureYear(request.getManufactureYear());

        Vehicle saved = vehicleRepo.save(vehicle);
        CustomerProfile customer = customerRepository.findById(saved.getCustomerId()).orElse(null);
        return buildVehicleResponse(saved, customer);
    }

    // ===== DELETE =====

    @Transactional
    public void deleteVehicle(Integer vehicleId) {
        log.info("Deleting vehicle: {}", vehicleId);
        vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe: " + vehicleId));
        vehicleRepo.deleteById(vehicleId);
    }

    // ===== Private helpers =====

    private VehicleListResponse.VehicleItem buildVehicleItem(Vehicle v) {
        VehicleListResponse.VehicleItem item = dtoMapper.toVehicleItem(v);

        odometerRepo.findLatestByVehicleId(v.getVehicleId())
                .ifPresent(o -> item.setLastOdometerReading(o.getReading()));

        serviceTicketRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("vehicleId"), v.getVehicleId()),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).stream().findFirst()
                .ifPresent(t -> item.setLastServiceDate(t.getCreatedAt().toLocalDate()));

        return item;
    }

    private VehicleResponse buildVehicleResponse(Vehicle v, CustomerProfile customer) {
        VehicleResponse response = dtoMapper.toResponse(v, customer);

        odometerRepo.findLatestByVehicleId(v.getVehicleId())
                .ifPresent(o -> response.setLastOdometerReading(o.getReading()));

        serviceTicketRepository.findAll(
                (root, query, cb) -> cb.equal(root.get("vehicleId"), v.getVehicleId()),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).stream().findFirst()
                .ifPresent(t -> response.setLastServiceDate(t.getCreatedAt().toLocalDate()));

        return response;
    }
}
