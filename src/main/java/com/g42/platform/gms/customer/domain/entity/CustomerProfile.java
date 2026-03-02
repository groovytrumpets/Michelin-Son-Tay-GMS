package com.g42.platform.gms.customer.domain.entity;

import com.g42.platform.gms.auth.entity.Gender;
import jakarta.persistence.*;
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
public class CustomerProfile {
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
