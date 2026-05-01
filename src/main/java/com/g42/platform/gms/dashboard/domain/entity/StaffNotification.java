package com.g42.platform.gms.dashboard.domain.entity;

import com.g42.platform.gms.dashboard.domain.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@AllArgsConstructor
public class StaffNotification {

    private Integer notificationId;
    private Integer staffId;
    private String title;
    private String message;
    private NotificationType notificationType;
    private Boolean isRead;
    private Integer sentBy;
    private LocalDateTime sentAt;
}
