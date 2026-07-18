package com.g42.platform.gms.chat.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "ChatMessageJpa")
@Table(name = "chat_message")
public class ChatMessageJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Integer messageId;

    @Column(name = "conversation_id", nullable = false)
    private Integer conversationId;

    @Column(name = "sender_id", nullable = false)
    private Integer senderId;

    @Column(name = "client_msg_id", length = 64, nullable = false)
    private String clientMsgId;

    @Column(name = "type", length = 10, nullable = false)
    private String type; // TEXT | IMAGE | VIDEO | FILE | STICKER

    @Column(name = "text", length = 2000)
    private String text;

    @Column(name = "sticker_id", length = 64)
    private String stickerId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
