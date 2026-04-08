package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import lombok.Data;

import java.util.List;

@Data
public class EditServiceTicketRequest {
    private String customerRequest;
    private String checkInNotes;
    private List<Integer> catalogItemIds;
}
