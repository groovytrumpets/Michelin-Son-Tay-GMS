package com.g42.platform.gms.booking_management.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Data
public class TimeSlot {
    private Integer slotId;
    private LocalTime startTime;
    private Integer capacity;
    private Boolean isActive;
    private String period;
}
