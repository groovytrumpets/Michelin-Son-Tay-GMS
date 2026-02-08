package com.g42.platform.gms.booking.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddBlacklistDto {
    
    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^0[0-9]{9,10}$", message = "Số điện thoại phải bắt đầu bằng 0 và có 10-11 chữ số")
    private String phone;
    
    @NotBlank(message = "Lý do chặn là bắt buộc")
    private String reason;
}
