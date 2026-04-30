package com.g42.platform.gms.dashboard.api.dto;

import com.g42.platform.gms.dashboard.domain.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationRespondDto {
    private Integer notificationId;
    private Integer staffId;
    private String title;
    private String message;
    private NotificationType notificationType;
    private Boolean isRead;
    private Integer sentBy;
    private LocalDateTime sentAt;
}
