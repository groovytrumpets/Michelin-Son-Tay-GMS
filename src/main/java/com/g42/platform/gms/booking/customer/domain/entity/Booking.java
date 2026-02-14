package com.g42.platform.gms.booking.customer.domain.entity;

import com.g42.platform.gms.booking.customer.domain.enums.BookingStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Booking {
    private Integer bookingId;
    private Integer customerId;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String serviceCategory;
    private BookingStatus status;
    private String description;
    private Boolean isGuest = false;
    private LocalDateTime createdAt;
    private List<Integer> serviceIds = new ArrayList<>();
    
    public void initializeDefaults() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = BookingStatus.PENDING;
        }
        if (isGuest == null) {
            isGuest = false;
        }
    }
}
