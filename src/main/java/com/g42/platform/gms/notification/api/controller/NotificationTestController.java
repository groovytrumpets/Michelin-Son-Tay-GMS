package com.g42.platform.gms.notification.api.controller;

import com.g42.platform.gms.notification.application.service.NotificationService;
import com.g42.platform.gms.notification.domain.NotificationSender;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/api/test/notify/")
public class NotificationTestController {
    private final NotificationService notificationSender;

    @PostMapping("/booking-confirmed")
    public String bookingConfirmed(){
        notificationSender.sendBookingCf(
                "MTS-???????",
                "NGUYEN VAN A",
                "10h"
        );
        return "OK";
    }
}
