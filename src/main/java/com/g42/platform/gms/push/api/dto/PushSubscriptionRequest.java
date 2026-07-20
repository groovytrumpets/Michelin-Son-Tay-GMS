package com.g42.platform.gms.push.api.dto;

import lombok.Data;

/**
 * Payload FE gửi khi bật thông báo (khớp PushSubscription.toJSON() của trình duyệt,
 * bọc thêm userAgent/deviceLabel). Xem FE src/services/pushService.js.
 */
@Data
public class PushSubscriptionRequest {
    private String endpoint;
    private Keys keys;
    private Long expirationTime;
    private String userAgent;
    private String deviceLabel;

    @Data
    public static class Keys {
        private String p256dh;
        private String auth;
    }
}
