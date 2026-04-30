package com.g42.platform.gms.dashboard.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

public class StaffNotification {

    private Integer notificationId;
    private Integer staffId;
    private String title;
    private String message;
    private String notificationType = "INFO";
    private Boolean isRead;
    private Integer sentBy;
    private LocalDateTime sentAt;
}
