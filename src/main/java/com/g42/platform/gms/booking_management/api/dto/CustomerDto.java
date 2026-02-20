package com.g42.platform.gms.booking_management.api.dto;

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
public class CustomerDto {
    private String fullName;
    private String phone;
    private LocalDateTime firstBookingAt;
}
