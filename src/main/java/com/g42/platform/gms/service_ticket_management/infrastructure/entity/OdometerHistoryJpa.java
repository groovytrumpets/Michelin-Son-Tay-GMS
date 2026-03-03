package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * JPA entity for odometer_history table.
 * 
 * Maps to the odometer_history table in the database.
 * This entity stores historical odometer readings for vehicles,
 * including rollback detection for fraud prevention.
 */
@Entity
@Table(name = "odometer_history", indexes = {
    @Index(name = "idx_vehicle", columnList = "vehicle_id"),
    @Index(name = "idx_recorded_at", columnList = "recorded_at")
})
@Data
public class OdometerHistoryJpa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reading_id")
    private Integer readingId;
    
    @Column(name = "vehicle_id", nullable = false)
    private Integer vehicleId;
    
    @Column(name = "reading", nullable = false)
    private Integer reading;
    
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;
    
    @Column(name = "recorded_by", nullable = false)
    private Integer recordedBy;
    
    @Column(name = "service_ticket_id")
    private Integer serviceTicketId;
    
    @Column(name = "rollback_detected")
    private Boolean rollbackDetected = false;
    
    @Column(name = "previous_reading")
    private Integer previousReading;
    
    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
    }
}
