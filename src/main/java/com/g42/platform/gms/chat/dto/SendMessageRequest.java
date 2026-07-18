package com.g42.platform.gms.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class SendMessageRequest {
    private Integer conversationId; // chỉ cần khi gửi qua STOMP (/app/chat.send); REST đã có {id} trên path
    @NotBlank
    private String clientMsgId;
    @NotBlank
    private String type; // text | image | video | file | sticker
    private String text;
    private List<AttachmentDto> attachments;
    private String stickerId;
}
