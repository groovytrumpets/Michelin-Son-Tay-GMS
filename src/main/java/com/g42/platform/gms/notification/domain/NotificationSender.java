package com.g42.platform.gms.notification.domain;

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Repository
public interface NotificationSender {
    void sendBookingCf(String s, String nguyenVanA, String s1);
}
