package com.g42.platform.gms.notification.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ZaloWebhookPayloadDto {
    @JsonProperty("event_name")
    private String eventName;

    @JsonProperty("app_id")
    private String appId;

    @JsonProperty("oa_id")
    private String oaId;

    private String timestamp;

    private ZaloFeedbackMessageDto message;
}
