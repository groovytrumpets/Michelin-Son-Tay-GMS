package com.g42.platform.gms.auth.dto;

import com.g42.platform.gms.auth.entity.Gender;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CustomerProfileResponse {
    private Integer customerId;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dob;
    private Gender gender;
    private String avatar;
    private LocalDateTime firstBookingAt;
    private LocalDateTime createdAt;
}
