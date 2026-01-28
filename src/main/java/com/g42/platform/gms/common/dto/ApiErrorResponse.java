package com.g42.platform.gms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiErrorResponse {
    private String code;      // MACHINE READABLE
    private String message;   // USER READABLE
}
