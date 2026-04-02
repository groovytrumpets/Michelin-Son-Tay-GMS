package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for service ticket list view (receptionist).
 * Tương tự BookedRespond trong booking_management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTicketListResponse {
    
    private Integer serviceTicketId;
    private String ticketCode;
    
    // Customer info
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    
    // Vehicle info
    private Integer vehicleId;
    private String licensePlate;
    private String vehicleMake;
    private String vehicleModel;
    
    // Booking info
    private Integer bookingId;
    private String bookingCode;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    
    // Service info
    private String serviceCategory;
    private String customerRequest;
    
    // Status
    private TicketStatus ticketStatus;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
    
    // Flags
    private Boolean isGuest;
    private Integer queueNumber;
}
