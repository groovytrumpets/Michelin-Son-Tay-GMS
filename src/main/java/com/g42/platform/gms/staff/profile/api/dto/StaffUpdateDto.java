package com.g42.platform.gms.staff.profile.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StaffUpdateDto {
    private String fullName;
    private String phone;
    private String position;
    private String avatar;
    private String status;
    private java.sql.Date dob;
    private List<RoleDto> roles;
}
