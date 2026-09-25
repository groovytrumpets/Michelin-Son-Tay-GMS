package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillKind;
import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillReviewStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.EntryMode;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
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
    private TicketType ticketType;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
    
    // Flags
    private Boolean isGuest;
    private Integer queueNumber;
    private Boolean hasBill;
    private Integer billId;

    // Bán cho khách lẻ vãng lai — FE gắn nhãn "Khách lẻ"; customerName/customerPhone
    // ở trên đã được thay bằng tên/SĐT ghi trên phiếu, không phải của hồ sơ dùng chung
    private Boolean isWalkIn;

    // Nhập bù phiếu ngày trước — FE gắn nhãn "Nhập bù" / "Chờ duyệt"
    private EntryMode entryMode;
    private BackfillKind backfillKind;
    private BackfillReviewStatus backfillReviewStatus;

    // Xưởng làm phiếu (Liquibase 044)
    private Integer branchId;
    private String branchName;
}
