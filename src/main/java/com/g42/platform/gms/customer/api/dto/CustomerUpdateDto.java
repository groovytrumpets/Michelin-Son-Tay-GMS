package com.g42.platform.gms.customer.api.dto;

import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.auth.entity.Gender;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerUpdateDto {
    private CustomerStatus status;
    private LocalDateTime lastLoginAt;

    private String fullName;
    private String phone;
    private String email;
    private LocalDate dob;
    private Gender gender;
    private String avatar;
    private LocalDateTime firstBookingAt;
}
