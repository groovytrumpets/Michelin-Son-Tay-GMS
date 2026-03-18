package com.g42.platform.gms.service_ticket_management.api.dto.assign;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private Integer roleId;
    private String roleCode;
    private String roleName;
}
