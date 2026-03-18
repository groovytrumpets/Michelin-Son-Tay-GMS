package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import lombok.Data;

import java.util.List;

@Data
public class SafetyInspectionRequest {
    
    private Integer serviceTicketId;
    private String generalNotes;
    private String technicianNotes;
    private TireInputRequest tires;
    private List<InspectionItemRequest> items;
}