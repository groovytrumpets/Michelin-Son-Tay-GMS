package com.g42.platform.gms.systemlog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogDto {
    private Long logId;
    private Integer actorStaffId;
    private String actorName;
    private String actorRole;
    private String action;
    private String module;
    private String severity;
    private String description;
    private String targetType;
    private String targetId;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
