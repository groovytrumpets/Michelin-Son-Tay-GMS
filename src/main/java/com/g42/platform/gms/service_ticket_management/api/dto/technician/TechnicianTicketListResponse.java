package com.g42.platform.gms.service_ticket_management.api.dto.technician;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for technician ticket list view.
 * Hiển thị danh sách phiếu cho kỹ thuật viên.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianTicketListResponse {
    
    private Integer serviceTicketId;
    private String ticketCode;
    private TicketStatus ticketStatus;
    
    // Thông tin xe
    private Integer vehicleId;
    private String licensePlate;
    private String vehicleMake;
    private String vehicleModel;
    
    // Thông tin khách hàng
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    
    // Thông tin booking
    private Integer bookingId;
    private String bookingCode;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    
    // Thông tin yêu cầu
    private String customerRequest;
    private String technicianNotes;
    
    // Timestamps
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
}
