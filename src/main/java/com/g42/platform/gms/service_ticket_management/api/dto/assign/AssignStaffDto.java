package com.g42.platform.gms.service_ticket_management.api.dto.assign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignStaffDto {
    private Integer staffId;
    private String roleInTicket;
    private Boolean isPrimary;
    private String note;
}
