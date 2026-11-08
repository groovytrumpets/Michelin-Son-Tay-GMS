package com.g42.platform.gms.notification.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Cấu hình ứng dụng Zalo OA — 1 dòng duy nhất, id = 1 (Liquibase 053-3).
 * Ô nào null/rỗng thì ZaloOaSettings rơi về application.properties.
 */
@Getter
@Setter
@Entity
@Table(name = "zalo_oa_config")
public class ZaloOaConfig {
    public static final int SINGLETON_ID = 1;

    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "app_id", length = 50)
    private String appId;

    @Column(name = "secret_key", length = 255)
    private String secretKey;

    @Column(name = "redirect_uri", length = 500)
    private String redirectUri;

    /** JSON {"OTP":"547094",...} */
    @Column(name = "template_map", columnDefinition = "TEXT")
    private String templateMap;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by_staff_id")
    private Integer updatedByStaffId;
}
