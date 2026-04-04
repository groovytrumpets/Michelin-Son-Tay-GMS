package com.g42.platform.gms.notification.domain;

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationSender {
    void sendBookingCf(String s, String nguyenVanA, String s1);
    void sendBookingConfirm(String phone, String customerName, List<String> productName, String orderCode, LocalDateTime bookingTime, String garageLocation);
}
