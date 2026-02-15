package com.g42.platform.gms.booking_management.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;

@Entity
@Table(name = "time_slot")
@Data
public class TimeSlotJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Integer slotId;

    @Column(name = "start_time", nullable = false, unique = true)
    private LocalTime startTime;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "period", length = 20)
    private String period;
}
