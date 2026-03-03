package com.g42.platform.gms.service_ticket_management.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Domain entity representing an Odometer Reading.
 * Simple POJO following the booking pattern.
 */
@Data
public class OdometerReading {
    
    private Integer readingId;
    private Integer vehicleId;
    private Integer reading;
    private LocalDateTime recordedAt;
    private Integer recordedBy;
    private Integer serviceTicketId;
    private Boolean rollbackDetected;
    private Integer previousReading;
}
