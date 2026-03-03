package com.g42.platform.gms.customer.domain.entity;

import com.g42.platform.gms.auth.entity.CustomerStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAuth {
    private Integer customerAuthId;
    private Integer customerId;
    private String pinHash;
    private CustomerStatus status;
    private Integer failedAttemptCount;
    private Integer otpAttemptCount;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
