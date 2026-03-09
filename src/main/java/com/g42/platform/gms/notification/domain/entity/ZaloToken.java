package com.g42.platform.gms.notification.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ZaloToken {
    private Long id;
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private Instant createdAt;
}
