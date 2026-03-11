package com.g42.platform.gms.service_ticket_management.api.dto.assignment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentInfo {
    private Integer assignmentId;
    private Integer staffId;
    private String staffName;
    private String staffPhone;
    private String roleInTicket;
    private Boolean isPrimary;
    private String note;
    private LocalDateTime assignedAt;
}
