package com.g42.platform.gms.booking.customer.api.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class AvailableSlotResponse {
    private LocalTime startTime;
    private String period;
    private Integer remainingCapacity;
    private Boolean isAvailable;
    private String status;
}
