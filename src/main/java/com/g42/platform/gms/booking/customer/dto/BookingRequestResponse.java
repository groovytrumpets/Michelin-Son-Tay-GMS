package com.g42.platform.gms.booking.customer.dto;

import com.g42.platform.gms.booking.customer.entity.BookingRequest;
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
    
    public static BookingRequestResponse from(BookingRequest request) {
        BookingRequestResponse response = new BookingRequestResponse();
        response.setRequestId(request.getRequestId());
        response.setPhone(request.getPhone());
        response.setFullName(request.getFullName());
        response.setScheduledDate(request.getScheduledDate());
        response.setScheduledTime(request.getScheduledTime());
        response.setDescription(request.getDescription());
        response.setStatus(request.getStatus().name());
        response.setCreatedAt(request.getCreatedAt());
        response.setExpiresAt(request.getExpiresAt());
        return response;
    }
}
