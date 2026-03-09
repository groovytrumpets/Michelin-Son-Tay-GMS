package com.g42.platform.gms.notification.application.service;

import com.g42.platform.gms.notification.domain.NotificationSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationSender notificationSender;

    public NotificationService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public void sendBookingCf(String s, String nguyenVanA, String s1) {
        notificationSender.sendBookingCf(s, nguyenVanA, s1);
    }
}
