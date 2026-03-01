package com.g42.platform.gms.booking.customer.api.dto;

import lombok.Data;

import java.time.LocalTime;

/**
 * Unified response for time slot information with full details
 */
@Data
public class TimeSlotResponse {
    private Integer slotId;
    private LocalTime startTime;
    private String period;
    private Integer capacity;
    private Boolean isActive;
    private Integer remainingCapacity;
    private Boolean isAvailable;
    private String status;
}
