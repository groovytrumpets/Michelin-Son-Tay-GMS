package com.g42.platform.gms.booking_management.api.dto.timeslot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlotBookedResponse {
    private Integer slotId;
    private LocalTime startTime;
    private String period;
    private Integer capacity;
    private Boolean isActive;
    private Integer remainingCapacity;
    private Boolean isAvailable;
    private String status;
    private List<SlotBookingInfoDto> bookings;
}
