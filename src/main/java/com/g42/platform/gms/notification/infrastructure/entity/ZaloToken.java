package com.g42.platform.gms.notification.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "zalo_token", schema = "michelin_garage")
public class ZaloToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idzalo_token", nullable = false)
    private Long id;


    @Column(name = "access_token")
    private String accessToken;


    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Size(max = 255)
    @Column(name = "code_verifier")
    private String codeVerifier;

    @Size(max = 40)
    @Column(name = "state", length = 40)
    private String state;


}