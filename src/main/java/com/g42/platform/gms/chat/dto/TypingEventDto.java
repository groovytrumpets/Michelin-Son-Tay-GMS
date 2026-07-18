package com.g42.platform.gms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingEventDto {
    private Integer conversationId;
    private Integer staffId;
    private boolean typing;
}
