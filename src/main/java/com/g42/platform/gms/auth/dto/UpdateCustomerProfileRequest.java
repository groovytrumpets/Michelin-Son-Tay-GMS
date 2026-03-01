package com.g42.platform.gms.auth.dto;

import com.g42.platform.gms.auth.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateCustomerProfileRequest {
    
    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email không được quá 100 ký tự")
    private String email;
    
    private Gender gender;
    
    @Size(max = 255, message = "URL avatar không được quá 255 ký tự")
    private String avatar;
}
