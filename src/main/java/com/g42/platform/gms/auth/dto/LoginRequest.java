package com.g42.platform.gms.auth.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @Pattern(
            regexp = "^((0|\\+84)[0-9]{9}|[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6})$",
            message = "Số điện thoại hoặc email không hợp lệ"
    )
    private String phone;
    private String pin;
}
