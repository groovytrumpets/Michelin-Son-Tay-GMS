package com.g42.platform.gms.estimation.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceReminder {
    private Integer reminderId;
    private Integer serviceTicketId;
    private Integer vehicleId;
    private Integer customerId;
    private Integer staffId;
    private LocalDate reminderDate;
    private LocalTime reminderTime;
    private String note;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private String reason;
    private Integer bookingId;


}