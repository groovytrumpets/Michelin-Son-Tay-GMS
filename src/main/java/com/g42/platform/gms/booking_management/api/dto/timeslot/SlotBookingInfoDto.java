package com.g42.platform.gms.booking_management.api.dto.timeslot;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SlotBookingInfoDto {
    private String bookingCode;
    private String fullName;
}
