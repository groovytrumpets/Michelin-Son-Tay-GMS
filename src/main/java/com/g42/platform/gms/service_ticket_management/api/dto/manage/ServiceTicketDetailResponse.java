package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Response DTO for service ticket detail view (receptionist).
 * Tương tự BookedDetailResponse trong booking_management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTicketDetailResponse {
    
    private Integer serviceTicketId;
    private String ticketCode;
    
    // Customer info
    private CustomerInfo customer;
    
    // Vehicle info
    private VehicleInfo vehicle;
    
    // Booking info
    private BookingInfo booking;
    
    // Service info
    private String serviceCategory;
    private String customerRequest;
    private List<ServiceInfo> services;
    
    // Check-in info
    private String checkInNotes;
    private Integer odometerReading;
    private List<PhotoInfo> photos;
    
    // Status & timestamps
    private TicketStatus ticketStatus;
    private LocalDateTime receivedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime estimatedDeliveryAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Staff info
    private Integer createdBy;
    private String createdByName;

    // Assignment info — advisor phụ trách
    private String advisorName;
    private Integer advisorId;

    // Warehouse export/allocation status
    private Boolean hasDraftStockIssue;
    private Boolean hasConfirmedStockIssue;
    private Boolean hasBill;
    private Integer billId;
    private Integer reservedAllocationCount;
    private Integer pendingIssueRequestAllocationCount;
    private Boolean canRequestIssueDraft;
    private Integer committedAllocationCount;
    private Boolean warehouseReadyForRepair;
    
    // Flags
    private Boolean immutable;
    private Boolean isGuest;
    private Boolean safetyInspectionEnabled;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerInfo {
        private Integer customerId;
        private String fullName;
        private String phone;
        private String email;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleInfo {
        private Integer vehicleId;
        private String licensePlate;
        private String make;
        private String model;
        private Integer year;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingInfo {
        private Integer bookingId;
        private String bookingCode;
        private LocalDate scheduledDate;
        private LocalTime scheduledTime;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ServiceInfo {
        private Integer serviceId;
        private String serviceName;
        private String category;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhotoInfo {
        private Integer photoId;
        private String category;
        private String photoUrl;
        private String description;
        private LocalDateTime uploadedAt;
    }
}
