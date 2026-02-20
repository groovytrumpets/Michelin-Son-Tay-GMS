package com.g42.platform.gms.booking_management.infrastructure.entity;

import com.g42.platform.gms.booking.customer.infrastructure.entity.BookingJpaEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "booking_slot_reservation")
@Data
public class BookingSlotReservationJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Integer reservationId;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private BookingJpaEntity booking;

    @Column(name = "reserved_date", nullable = false)
    private LocalDate reservedDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
}
