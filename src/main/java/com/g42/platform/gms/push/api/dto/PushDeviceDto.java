package com.g42.platform.gms.push.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/** Thông tin thiết bị đã đăng ký (cho GET /api/push/subscriptions). */
@Data
@AllArgsConstructor
public class PushDeviceDto {
    private Long id;
    private String userAgent;
    private String deviceLabel;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
