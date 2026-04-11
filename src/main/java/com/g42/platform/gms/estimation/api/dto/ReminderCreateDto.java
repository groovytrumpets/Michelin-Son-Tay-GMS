package com.g42.platform.gms.estimation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReminderCreateDto {
    private Integer serviceTicketId;
    private Integer vehicleId;
    private Integer customerId;
    private LocalDate reminderDate;
    private LocalTime reminderTime;
    private String note;
}
