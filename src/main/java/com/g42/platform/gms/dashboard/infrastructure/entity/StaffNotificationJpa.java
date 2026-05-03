package com.g42.platform.gms.dashboard.infrastructure.entity;

import com.g42.platform.gms.dashboard.domain.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_notification")
@Data
public class StaffNotificationJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "staff_id")
    private Integer staffId; // NULL = broadcast to all

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 20)
    private NotificationType notificationType;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "sent_by", nullable = false)
    private Integer sentBy;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Size(max = 255)
    @Column(name = "url")
    private String url;
}
