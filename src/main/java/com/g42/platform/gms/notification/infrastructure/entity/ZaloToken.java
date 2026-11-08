package com.g42.platform.gms.notification.infrastructure.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Token gửi tin của Zalo OA. Dòng có state = "active" là token đang dùng — chỉ được có 1 dòng
 * như vậy (ZaloTokenService giữ bất biến này khi dán tay / kết nối lại).
 * Các dòng state khác ("verified", "inactive") là rác của luồng OAuth cũ, không ai đọc.
 */
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

    // ---- Liquibase 053-2 ----
    @Column(name = "refresh_token_expires_at")
    private Instant refreshTokenExpiresAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by_staff_id")
    private Integer updatedByStaffId;

    @Column(name = "last_refresh_at")
    private Instant lastRefreshAt;

    @Column(name = "last_refresh_error", length = 500)
    private String lastRefreshError;

    /** OAUTH | MANUAL */
    @Column(name = "source", length = 20)
    private String source;
}
