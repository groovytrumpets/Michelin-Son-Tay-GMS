package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for service_ticket table.
 * 
 * Maps to the service_ticket table in the database.
 * This entity represents a service ticket created during vehicle check-in.
 */
@Entity(name = "ServiceTicketManagement")
@Table(name = "service_ticket")
@Data
public class ServiceTicketJpa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_ticket_id")
    private Integer serviceTicketId;
    
    @Column(name = "ticket_code", length = 50, unique = true)
    private String ticketCode;
    
    @Column(name = "booking_id")
    private Integer bookingId;
    
    @Column(name = "vehicle_id", nullable = false)
    private Integer vehicleId;
    
    @Column(name = "customer_id", nullable = false)
    private Integer customerId;
    
    @Column(name = "created_by")
    private Integer createdBy;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_status", length = 50)
    private TicketStatus ticketStatus;
    
    @Column(name = "received_at")
    private LocalDateTime receivedAt;
    
    @Column(name = "immutable")
    private Boolean immutable = false;
    
    @Column(name = "customer_request", columnDefinition = "TINYTEXT")
    private String customerRequest;
    
    @Column(name = "technician_notes", columnDefinition = "TINYTEXT")
    private String technicianNotes;
    
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "estimated_delivery_at")
    private LocalDateTime estimatedDeliveryAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    @Column(name = "check_in_notes", columnDefinition = "TEXT")
    private String checkInNotes;

    @Column(name = "safety_inspection_enabled")
    private Boolean safetyInspectionEnabled = false;

    @Column(name = "is_printed")
    private Boolean isPrinted = false;

    @Column(name = "printed_at")
    private LocalDateTime printedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @OneToMany(mappedBy = "serviceTicketId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VehicleConditionPhotoJpa> conditionPhotos = new ArrayList<>();
    
    @OneToMany(mappedBy = "serviceTicketId", fetch = FetchType.LAZY)
    private List<ServiceTicketAssignmentJpa> assignments = new ArrayList<>();
    @Column(name = "queue_number")
    private Integer queueNumber;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
