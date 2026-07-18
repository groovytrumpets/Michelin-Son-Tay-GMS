package com.g42.platform.gms.systemlog.service;

import lombok.Builder;
import lombok.Value;

/**
 * Giá trị bất biến chứa toàn bộ ngữ cảnh audit, được resolve trên thread gọi
 * (SecurityContext/RequestAttributes không tồn tại trên thread async).
 */
@Value
@Builder
public class AuditRecord {
    Integer actorStaffId;
    String actorName;
    String actorRole;
    String action;
    String module;
    String severity;
    String description;
    String targetType;
    String targetId;
    String ipAddress;
    String userAgent;
}
