package com.g42.platform.gms.booking.customer.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingResponse {
    private Integer bookingId;
    private String bookingCode;
    private Integer customerId;
    private String customerName;
    private String phone;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String description;
    private String status;
    private Boolean isGuest;
    private List<Integer> serviceIds;
    private List<ServiceItemDto> services;
    private Integer totalEstimatedTime;

    // Progress tracking
    private List<ProgressStep> progressSteps;
    private String technicianNotes;
    private String ticketStatus; // null nếu chưa check-in

    @Data
    public static class ProgressStep {
        private String label;
        private String status; // COMPLETED, ACTIVE, PENDING
    }
}
