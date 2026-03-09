package com.g42.platform.gms.service_ticket_management.api.dto.technician;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Response DTO for technician ticket detail view.
 * Hiển thị chi tiết phiếu cho kỹ thuật viên.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianTicketDetailResponse {
    
    private Integer serviceTicketId;
    private String ticketCode;
    
    // Thông tin khách hàng
    private CustomerInfo customer;
    
    // Thông tin xe
    private VehicleInfo vehicle;
    
    // Thông tin booking
    private BookingInfo booking;
    
    // Thông tin dịch vụ
    private String serviceCategory;
    private String customerRequest;
    private List<ServiceInfo> services;
    
    // Thông tin check-in
    private String checkInNotes;
    private Integer odometerReading;
    private List<PhotoInfo> photos;
    
    // Ghi chú kỹ thuật viên
    private String technicianNotes;
    
    // Trạng thái & timestamps
    private TicketStatus ticketStatus;
    private LocalDateTime receivedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Thông tin nhân viên
    private Integer createdBy;
    private String createdByName;
    
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
