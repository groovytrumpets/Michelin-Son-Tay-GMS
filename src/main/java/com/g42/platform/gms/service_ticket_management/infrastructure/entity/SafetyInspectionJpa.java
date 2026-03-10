package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.InspectionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity for safety_inspection table.
 */
@Entity
@Table(name = "safety_inspection")
@Data
public class SafetyInspectionJpa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inspection_id")
    private Integer inspectionId;
    
    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;
    
    @Column(name = "technician_id")
    private Integer technicianId;
    
    @Column(name = "general_notes", columnDefinition = "TEXT")
    private String generalNotes;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_status", length = 20)
    private InspectionStatus inspectionStatus;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "inspectionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SafetyInspectionTireJpa> tires = new ArrayList<>();
    
    @OneToMany(mappedBy = "inspectionId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SafetyInspectionItemJpa> items = new ArrayList<>();
    
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
