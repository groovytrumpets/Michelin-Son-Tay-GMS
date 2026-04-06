package com.g42.platform.gms.notification.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
@Getter
@AllArgsConstructor
public class ZaloFeedbackMessageDto {
    private String note;

    private Integer rate; // Số sao đánh giá (1-5)

    @JsonProperty("submit_time")
    private String submitTime;

    @JsonProperty("msg_id")
    private String msgId;

    private List<String> feedbacks; // Mảng các lời khen/chê

    @JsonProperty("tracking_id")
    private String trackingId; // MÃ PHIẾU SỬA CHỮA (Rất quan trọng!)
}
