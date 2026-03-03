package com.g42.platform.gms.auth.dto;

import com.g42.platform.gms.auth.entity.Role;
import com.g42.platform.gms.auth.entity.StaffRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class StaffAuthResponse {
    private String fullName;
    private String avatarUrl;
    private String message;
    private List<String> role;
    private String token;
}