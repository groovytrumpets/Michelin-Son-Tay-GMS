package com.g42.platform.gms.notification.application.service;

import com.g42.platform.gms.notification.domain.NotificationSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private final NotificationSender notificationSender;

    public NotificationService(NotificationSender notificationSender) {
        this.notificationSender = notificationSender;
    }

    public void sendBookingCf(String phone, String name, List<String> productName, String orderCode, LocalDateTime bookingTime, String garageLocation) {
        notificationSender.sendBookingConfirm(phone, name, productName,orderCode,bookingTime,garageLocation);
    }

    public void sendOtpViaZalo(String number,String otp) {
        notificationSender.sendOtpVerify(number,otp);
    }

    public void sendFeedbackViaZalo(String number, String name, String code) {
        notificationSender.sendFeedback(number,name,code);
    }
}
