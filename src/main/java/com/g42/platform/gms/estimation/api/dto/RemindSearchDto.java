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
public class RemindSearchDto {
    private Integer reminderId;

    private Integer customerId;
    private String customerName;
    private String customerPhone;

    private Integer vehicleId;
    private String licensePlate;

    private Integer serviceTicketId;
    private String ticketCode;

    private LocalDate reminderDate;
    private LocalTime reminderTime;
    private String note;

    private String status;
    private String statusReason;
    private String advisorName;
}
