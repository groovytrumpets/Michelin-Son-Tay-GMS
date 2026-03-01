package com.g42.platform.gms.booking.customer.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AvailableSlotsResponse {
    private LocalDate date;
    private List<TimeSlotResponse> slots;
}
