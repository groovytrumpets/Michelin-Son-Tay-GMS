package com.g42.platform.gms.service_ticket_management.api.dto.assignment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketAssignmentResponse {
    private Integer ticketId;
    private AssignmentInfo primary;
    private List<AssignmentInfo> assistants;
}
