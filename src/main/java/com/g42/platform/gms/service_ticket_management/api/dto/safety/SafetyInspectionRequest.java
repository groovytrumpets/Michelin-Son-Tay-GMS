package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import com.g42.platform.gms.service_ticket_management.domain.enums.InspectionStatus;
import lombok.Data;

import java.util.List;

@Data
public class SafetyInspectionRequest {
    
    private Integer serviceTicketId;
    private String generalNotes;
    private String technicianNotes;
    private InspectionStatus inspectionStatus;
    private TireInputRequest tires;
    private List<InspectionItemRequest> items;
}
