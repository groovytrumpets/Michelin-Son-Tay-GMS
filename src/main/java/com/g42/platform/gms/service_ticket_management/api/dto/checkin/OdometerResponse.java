package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO containing odometer reading information.
 * Includes warning if rollback is detected.
 */
@Data
public class OdometerResponse {
    
    private Integer readingId;
    private Integer vehicleId;
    private Integer reading;
    private LocalDateTime recordedAt;
    private Integer recordedBy;
    private Boolean rollbackDetected;
    private Integer previousReading;
    private String warningMessage;  // Set if rollback detected
}
