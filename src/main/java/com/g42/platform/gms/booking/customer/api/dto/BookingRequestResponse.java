package com.g42.platform.gms.booking.customer.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class BookingRequestResponse {
    private Integer requestId;
    private String phone;
    private String fullName;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
