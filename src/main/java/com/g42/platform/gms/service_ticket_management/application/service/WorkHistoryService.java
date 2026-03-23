package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.service_ticket_management.api.dto.work_history.WorkHistoryResponse;
import com.g42.platform.gms.service_ticket_management.api.mapper.WorkHistoryApiMapper;
import com.g42.platform.gms.service_ticket_management.domain.entity.ServiceTicket;
import com.g42.platform.gms.service_ticket_management.domain.repository.ServiceTicketRepo;
import com.g42.platform.gms.service_ticket_management.domain.repository.WorkCategoryRepo;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import com.g42.platform.gms.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    
    private final ServiceTicketRepo serviceTicketRepo;
    private final VehicleRepository vehicleRepository;
    private final CustomerProfileRepository customerRepository;
    private final WorkCategoryRepo workCategoryRepository;
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
        
        // Execute query with pagination
        Page<ServiceTicket> ticketPage = serviceTicketRepo.findByTechnicianCompleted(technicianId, startDate, endDate, licensePlate, pageable);

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
    private WorkHistoryResponse mapToWorkHistoryResponse(ServiceTicket ticket) {
        Vehicle vehicle = vehicleRepository.findById(ticket.getVehicleId())
            .orElseThrow(() -> new IllegalStateException(
                "Vehicle not found for service ticket: " + ticket.getTicketCode()));

        CustomerProfile customer = customerRepository.findById(ticket.getCustomerId())
            .orElseThrow(() -> new IllegalStateException(
                "Customer not found for service ticket: " + ticket.getTicketCode()));

        String serviceType = resolveServiceType();

        return workHistoryMapper.toWorkHistoryResponse(ticket, vehicle, customer, serviceType);
    }

    private String resolveServiceType() {
        List<String> workCategories = workCategoryRepository.findDefaultWorkCategoryNames();
        if (workCategories == null || workCategories.isEmpty()) return "Service";
        return String.join(", ", workCategories);
    }
    
    // TODO: Export functionality
    // Future implementation will include methods to export work history to Excel (.xlsx) and PDF formats
}
