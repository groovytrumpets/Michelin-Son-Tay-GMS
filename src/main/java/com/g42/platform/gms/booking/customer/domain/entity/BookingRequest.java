package com.g42.platform.gms.booking.customer.domain.entity;

import com.g42.platform.gms.booking.customer.domain.enums.BookingRequestStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class BookingRequest {
    private Integer requestId;
    private String requestCode;
    private String phone;
    private String fullName;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String description;
    private String serviceCategory;
    private BookingRequestStatus status = BookingRequestStatus.PENDING;
    private Boolean isGuest = true;
    private Integer customerId;
    private Integer confirmedBy;
    private LocalDateTime confirmedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String clientIp;
    private List<Integer> serviceIds = new ArrayList<>();
    
    public void initializeDefaults() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingRequestStatus.PENDING;
        }
        if (isGuest == null) {
            isGuest = true;
        }
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusHours(24);
        }
    }
}
