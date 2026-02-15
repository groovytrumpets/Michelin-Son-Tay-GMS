package com.g42.platform.gms.booking_management.domain.entity;

import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingJpaEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class BookingSlotReservation {
    private Integer reservationId;
    private BookingJpaEntity booking;
    private LocalDate reservedDate;
    private LocalTime startTime;
}
