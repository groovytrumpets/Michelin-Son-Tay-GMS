package com.g42.platform.gms.booking.customer.domain.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SlotReservation {
    private Integer reservationId;
    private Integer bookingId;
    private LocalDate reservedDate;
    private LocalTime startTime;
}
