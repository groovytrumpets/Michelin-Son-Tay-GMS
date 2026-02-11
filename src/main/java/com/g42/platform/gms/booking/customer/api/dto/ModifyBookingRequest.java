package com.g42.platform.gms.booking.customer.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class ModifyBookingRequest {

    private LocalDate newAppointmentDate;
    private LocalTime newAppointmentTime;
    private String newUserNote;
    private List<Integer> newServiceIds;
}

