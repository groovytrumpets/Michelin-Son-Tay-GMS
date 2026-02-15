package com.g42.platform.gms.booking.customer.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class BookingResponse {
    private Integer bookingId;
    private Integer customerId;
    private String customerName;
    private String phone;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private String description;
    private String status;
    private Boolean isGuest;
    private List<Integer> serviceIds;
}
