package com.g42.platform.gms.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

    private String phone;
    private String otp;
}
