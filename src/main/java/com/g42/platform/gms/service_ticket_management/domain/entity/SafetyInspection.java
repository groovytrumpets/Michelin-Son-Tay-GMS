package com.g42.platform.gms.service_ticket_management.domain.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.InspectionStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a Safety Inspection.
 * Simple POJO following Clean Architecture - business logic in Service layer.
 */
@Data
public class SafetyInspection {
    
    private Integer inspectionId;
    private Integer serviceTicketId;
    private Integer technicianId;
    private String generalNotes;
    private InspectionStatus inspectionStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<SafetyInspectionTire> tires = new ArrayList<>();
    private List<SafetyInspectionItem> items = new ArrayList<>();
    
    /**
     * Initialize default values.
     */
    public void initializeDefaults() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (inspectionStatus == null) {
            inspectionStatus = InspectionStatus.PENDING;
        }
    }
}
