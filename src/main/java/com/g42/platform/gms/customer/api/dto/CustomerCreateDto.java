package com.g42.platform.gms.customer.api.dto;

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
    private String gender;
    private String dob;
    private String avatar;
}
