package com.g42.platform.gms.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private Integer messageId;
    private Integer conversationId;
    private String clientMsgId;
    private Integer senderId;
    private String senderName;
    private String senderAvatar;
    private String type; // text | image | video | file | sticker
    private String text;
    private List<AttachmentDto> attachments;
    private String stickerId;
    private LocalDateTime createdAt;
    private String status; // sent | read
}
