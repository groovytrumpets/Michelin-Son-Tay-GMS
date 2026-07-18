package com.g42.platform.gms.chat.dto;

import lombok.Data;

@Data
public class TypingRequest {
    private Integer conversationId;
    private boolean typing;
}
