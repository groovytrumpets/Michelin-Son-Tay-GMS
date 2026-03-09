package com.g42.platform.gms.notification.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "zalo_token", schema = "michelin_garage")
public class ZaloTokenJpa {
    @Id
    @Column(name = "idzalo_token", nullable = false)
    private Long id;

    @Size(max = 255)
    @Column(name = "accessToken")
    private String accessToken;

    @Size(max = 255)
    @Column(name = "refreshToken")
    private String refreshToken;

    @Column(name = "expiresAt")
    private Instant expiresAt;

    @Column(name = "createdAt")
    private Instant createdAt;


}