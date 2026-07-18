package com.g42.platform.gms.chat.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity(name = "ChatConversationParticipantJpa")
@Table(name = "chat_conversation_participant")
public class ConversationParticipantJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "conversation_id", nullable = false)
    private Integer conversationId;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "last_read_message_id")
    private Integer lastReadMessageId;
}
