package com.g42.platform.gms.auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetupPinRequest {
    private String phone;
    private String pin;
    private String confirmPin;

    /**
     * Khách có nhiều số: số đã chọn nhận OTP ở bước kích hoạt/quên PIN. Trùng với số vừa
     * xác thực OTP thành công thì số đó trở thành số chính của tài khoản.
     */
    private String phoneKey;
}
