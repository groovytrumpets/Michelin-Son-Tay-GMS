package com.g42.platform.gms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDto {
    private Integer conversationId;
    private String type; // direct | group
    private String title;
    private String avatarUrl;
    private List<ContactDto> participants; // KHÔNG bao gồm staff đang gọi API
    private MessageDto lastMessage;
    private long unreadCount;
    private LocalDateTime updatedAt;
}
