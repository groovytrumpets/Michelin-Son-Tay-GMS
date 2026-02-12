package com.g42.platform.gms.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_auth")
@Getter
@Setter
public class CustomerAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_auth_id")
    private Integer customerAuthId;

    @Column(name = "customer_id")
    private Integer customerId;

    @Column(name = "pin_hash")
    private String pinHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CustomerStatus status;

    @Column(name = "failed_attempt_count")
    private Integer failedAttemptCount;

    @Column(name = "otp_attempt_count")
    private Integer otpAttemptCount;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
