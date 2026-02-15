package com.g42.platform.gms.booking_management.api.dto.confirmed;

import java.time.LocalTime;

public class TimeSlotBookedRespond {
    private Integer slotId;
    private LocalTime startTime;
    private Integer capacity;
    private Boolean isActive;
    private String period;
}
