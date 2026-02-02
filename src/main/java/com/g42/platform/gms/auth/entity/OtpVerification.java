package com.g42.platform.gms.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_verification")
@Getter
@Setter
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "otp_id")
    private Integer otpId;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type")
    private AuthType authType;   // CUSTOMER

    @Column(name = "auth_id")
    private Integer authId;

    @Column(name = "otp_hash")
    private String otpHash;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
