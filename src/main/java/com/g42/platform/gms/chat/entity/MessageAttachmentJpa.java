package com.g42.platform.gms.chat.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity(name = "ChatMessageAttachmentJpa")
@Table(name = "chat_message_attachment")
public class MessageAttachmentJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Integer attachmentId;

    @Column(name = "message_id", nullable = false)
    private Integer messageId;

    @Column(name = "url", length = 500, nullable = false)
    private String url;

    @Column(name = "kind", length = 10, nullable = false)
    private String kind; // IMAGE | VIDEO | FILE

    @Column(name = "name")
    private String name;

    @Column(name = "size")
    private Long size;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;
}
