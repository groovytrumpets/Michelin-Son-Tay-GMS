package com.g42.platform.gms.service_ticket_management.api.dto.assign;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignStaffDto {
    private Integer assignmentId;
    private Integer serviceTicketId;
    private Integer staffId;
    private String fullName;
    private String roleInTicket;
    private Instant assignedAt;
    private Boolean isPrimary;
    private String status;
    private String note;
}


