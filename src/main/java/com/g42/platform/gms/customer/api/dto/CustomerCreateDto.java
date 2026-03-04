package com.g42.platform.gms.customer.api.dto;

import com.g42.platform.gms.auth.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerCreateDto {
    private String fullName;
    private String phone;
    private String email;
    private String pin;
    private Gender gender;
    private String dob;
    private String avatar;
}
