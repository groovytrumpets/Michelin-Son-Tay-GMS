package com.g42.platform.gms.auth.dto;

import com.g42.platform.gms.auth.entity.StaffRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.management.relation.Role;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class StaffAuthResponse {
    private String message;
    private List<StaffRole> role;
    private String token;
}