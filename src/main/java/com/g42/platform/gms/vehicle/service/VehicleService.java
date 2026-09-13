package com.g42.platform.gms.vehicle.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.OdometerHistoryJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.OdometerHistoryRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.vehicle.dto.VehicleListResponse;
import com.g42.platform.gms.vehicle.dto.VehicleUpdateRequest;
import com.g42.platform.gms.vehicle.dto.VehicleUpdateResponse;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {
    
    private final VehicleRepository vehicleRepo;
    private final CustomerProfileRepository customerRepository;
    private final OdometerHistoryRepository odometerRepository;
    private final ServiceTicketRepository serviceTicketRepository;

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
            
            // Get last service date
            List<ServiceTicketJpa> tickets = serviceTicketRepository.findAll();
            ServiceTicketJpa lastTicket = null;
            for (ServiceTicketJpa ticket : tickets) {
                if (vehicle.getVehicleId().equals(ticket.getVehicleId())) {
                    if (lastTicket == null || ticket.getCreatedAt().isAfter(lastTicket.getCreatedAt())) {
                        lastTicket = ticket;
                    }
                }
            }
            if (lastTicket != null) {
                info.setLastServiceDate(lastTicket.getCreatedAt().toLocalDate());
            }
            
            vehicleInfos.add(info);
        }
        
        response.setVehicles(vehicleInfos);

        log.info("Found {} vehicles for customer: {}", vehicleInfos.size(), customerId);
        return response;
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
        vehicleRepo.findByLicensePlate(newPlate).ifPresent(other -> {
            if (!other.getVehicleId().equals(vehicleId)) {
                throw new RuntimeException("Biển số xe đã tồn tại: " + newPlate);
            }
        });

        vehicle.setLicensePlate(newPlate);
        vehicle.setBrand(request.getBrand() != null ? request.getBrand().trim() : null);
        vehicle.setModel(request.getModel() != null ? request.getModel().trim() : null);
        vehicle.setManufactureYear(request.getManufactureYear());
        vehicle = vehicleRepo.save(vehicle);

        VehicleUpdateResponse response = new VehicleUpdateResponse();
        response.setVehicleId(vehicle.getVehicleId());
        response.setLicensePlate(vehicle.getLicensePlate());
        response.setBrand(vehicle.getBrand());
        response.setModel(vehicle.getModel());
        response.setManufactureYear(vehicle.getManufactureYear());
        return response;
    }
}