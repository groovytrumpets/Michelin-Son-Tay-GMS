package com.g42.platform.gms.service_ticket_management.domain.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a Service Ticket.
 * Simple POJO following the booking pattern - business logic in Service layer.
 */
@Data
public class ServiceTicket {
    
    private Integer serviceTicketId;
    private String ticketCode;
    private Integer bookingId;
    private Integer vehicleId;
    private Integer customerId;
    private TicketStatus ticketStatus;
    private Integer odometerReading;
    private String licensePlatePhotoUrl;
    private String checkInNotes;
    private Boolean immutable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // List of photo IDs (not full objects - MapStruct will handle conversion)
    private List<Integer> photoIds = new ArrayList<>();
    
    /**
     * Initialize default values.
     * Sets status to DRAFT for new tickets (check-in in progress).
     */
    public void initializeDefaults() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (ticketStatus == null) {
            ticketStatus = TicketStatus.DRAFT;  // Start as DRAFT, complete check-in will change to CREATED
        }
        if (immutable == null) {
            immutable = false;
        }
    }
}
