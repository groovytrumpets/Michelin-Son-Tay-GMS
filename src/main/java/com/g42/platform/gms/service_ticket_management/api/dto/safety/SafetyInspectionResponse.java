package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import com.g42.platform.gms.service_ticket_management.domain.enums.InspectionStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SafetyInspectionResponse {
    
    private Integer inspectionId;
    private Integer serviceTicketId;
    private Integer technicianId;
    private String generalNotes;
    private String technicianNotes;
    private InspectionStatus inspectionStatus;
    private List<TireDataResponse> tires;
    private List<InspectionItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}