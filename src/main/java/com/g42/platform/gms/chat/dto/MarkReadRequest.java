package com.g42.platform.gms.chat.dto;

import lombok.Data;

@Data
public class MarkReadRequest {
    private Integer conversationId; // chỉ cần khi gửi qua STOMP
    private Integer upToMessageId;
}
