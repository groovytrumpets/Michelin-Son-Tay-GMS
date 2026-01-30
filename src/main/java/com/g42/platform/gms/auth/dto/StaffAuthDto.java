package com.g42.platform.gms.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class StaffAuthDto {
    private Long staffAuthId;
    private Long staffId;
    private String email;
    private String passwordHash;
}
