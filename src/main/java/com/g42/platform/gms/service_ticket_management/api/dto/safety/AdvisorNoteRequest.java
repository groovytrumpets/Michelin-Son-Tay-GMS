package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import lombok.Data;

import java.util.List;

@Data
public class AdvisorNoteRequest {

    private List<AdvisorNoteItemRequest> items;
}
