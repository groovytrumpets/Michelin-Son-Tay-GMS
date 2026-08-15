package com.g42.platform.gms.booking.customer.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookingScheduleConfigResponse {
    private BookingConfigDto bookingConfig;
    private List<WorkingHoursDto> workingHours;
}
