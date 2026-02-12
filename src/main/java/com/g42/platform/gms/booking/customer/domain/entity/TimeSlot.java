package com.g42.platform.gms.booking.customer.domain.entity;

import lombok.Data;

import java.time.LocalTime;

@Data
public class TimeSlot {
    private Integer slotId;
    private LocalTime startTime;
    private Integer capacity;
    private Boolean isActive;
    private String period;
}
