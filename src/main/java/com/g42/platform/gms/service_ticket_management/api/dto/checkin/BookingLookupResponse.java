package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Response DTO containing booking information for check-in.
 * Includes customer details, scheduled time, and services.
 * 
 * Note: Vehicle information is now retrieved via separate API:
 * GET /api/receptionist/check-in/customers/{customerId}/vehicles
 */
@Data
public class BookingLookupResponse {
    
    // Booking information
    private Integer bookingId;
    private String bookingCode;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String serviceCategory;
    private String description;
    
    // Customer information
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    
    // Services
    private List<ServiceInfo> services;
    
    @Data
    public static class ServiceInfo {
        private Integer serviceId;
        private String serviceName;
        private String category;
    }
}
