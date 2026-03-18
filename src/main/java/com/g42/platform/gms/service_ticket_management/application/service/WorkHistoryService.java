package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.service_ticket_management.api.dto.work_history.WorkHistoryResponse;
import com.g42.platform.gms.service_ticket_management.api.mapper.WorkHistoryApiMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.WorkCategoryRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.specification.WorkHistorySpecification;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service class for managing technician work history.
 * 
 * This service provides functionality to retrieve and display completed
 * service tickets for a specific technician, with filtering capabilities
 * by date range and license plate.
 * 
 * Business Rules:
 * - Only returns service tickets with status = COMPLETED
 * - Only returns tickets assigned to the current technician with role = TECHNICIAN
 * - Validates date range (startDate must not be after endDate)
 * - Supports pagination for large result sets
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkHistoryService {
    
    private final ServiceTicketRepository serviceTicketRepository;
    private final VehicleRepository vehicleRepository;
    private final CustomerProfileRepository customerRepository;
    private final WorkCategoryRepository workCategoryRepository;
    private final WorkHistoryApiMapper workHistoryMapper;
    
    /**
     * Lấy danh sách lịch sử công việc
     * Chỉ lấy các service ticket có status = COMPLETED
     * Chỉ lấy các ticket được assign cho technician hiện tại
     * 
     * @param technicianId ID của kỹ thuật viên
     * @param startDate Ngày bắt đầu (inclusive)
     * @param endDate Ngày kết thúc (inclusive)
     * @param licensePlate Biển số xe (optional, case-insensitive)
     * @param pageable Pagination parameters
     * @return Page of WorkHistoryResponse DTOs
     * @throws IllegalArgumentException if startDate is after endDate
     */
    public Page<WorkHistoryResponse> getWorkHistory(
        Integer technicianId,
        LocalDate startDate,
        LocalDate endDate,
        String licensePlate,
        Pageable pageable
    ) {
        // Validate date range
        validateDateRange(startDate, endDate);
        
        // Build specification
        Specification<ServiceTicketJpa> spec = Specification.where(null);
        spec = spec.and(WorkHistorySpecification.byTechnicianId(technicianId));
        spec = spec.and(WorkHistorySpecification.isCompleted());
        spec = spec.and(WorkHistorySpecification.completedBetween(startDate, endDate));
        
        if (licensePlate != null && !licensePlate.isBlank()) {
            spec = spec.and(WorkHistorySpecification.byLicensePlate(licensePlate));
        }
        
        // Execute query with pagination
        Page<ServiceTicketJpa> ticketPage = serviceTicketRepository.findAll(spec, pageable);
        
        // Map to response DTOs
        return ticketPage.map(this::mapToWorkHistoryResponse);
    }
    
    /**
     * Validate that startDate is not after endDate.
     * 
     * @param startDate The start date
     * @param endDate The end date
     * @throws IllegalArgumentException if startDate is after endDate (ER021)
     */
    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid Date Range (ER021): Start date cannot be after end date");
        }
    }
    
    /**
     * Map a ServiceTicketJpa entity to WorkHistoryResponse DTO.
     * 
     * This method fetches related entities (Vehicle, CustomerProfile) and
     * resolves the service type, then uses the mapper to create the response DTO.
     * 
     * @param ticket The service ticket entity
     * @return WorkHistoryResponse DTO
     */
    private WorkHistoryResponse mapToWorkHistoryResponse(ServiceTicketJpa ticket) {
        // Fetch vehicle - throw exception if not found
        Vehicle vehicle = vehicleRepository.findById(ticket.getVehicleId())
            .orElseThrow(() -> new IllegalStateException(
                "Vehicle not found for service ticket: " + ticket.getTicketCode()));
        
        // Fetch customer - throw exception if not found
        CustomerProfile customer = customerRepository.findById(ticket.getCustomerId())
            .orElseThrow(() -> new IllegalStateException(
                "Customer not found for service ticket: " + ticket.getTicketCode()));
        
        // Resolve service type
        String serviceType = resolveServiceType(ticket);
        
        // Map to response
        return workHistoryMapper.toWorkHistoryResponse(ticket, vehicle, customer, serviceType);
    }
    
    /**
     * Resolve the service type for a service ticket.
     * 
     * Lấy danh sách 13 hạng mục kiểm tra an toàn default (is_default = 1).
     * Đây là danh sách cố định, không phụ thuộc vào service ticket cụ thể.
     * 
     * @param ticket The service ticket
     * @return Service type string (comma-separated default work category names)
     */
    private String resolveServiceType(ServiceTicketJpa ticket) {
        // Query all default work categories (13 items with is_default = 1)
        List<String> workCategories = workCategoryRepository.findDefaultWorkCategoryNames();
        
        // If no work categories found, return placeholder
        if (workCategories == null || workCategories.isEmpty()) {
            return "Service";
        }
        
        // Join work category names with comma
        return String.join(", ", workCategories);
    }
    
    // TODO: Export functionality
    // Future implementation will include methods to export work history to Excel (.xlsx) and PDF formats
}
