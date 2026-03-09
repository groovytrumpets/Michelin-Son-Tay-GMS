package com.g42.platform.gms.notification.api.controller;

import com.g42.platform.gms.notification.application.service.NotificationService;
import com.g42.platform.gms.notification.domain.NotificationSender;
import com.g42.platform.gms.notification.infrastructure.PKCE;
import com.g42.platform.gms.notification.infrastructure.ZaloOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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
    private ZaloOAuthService zaloOAuthService;
    @GetMapping("/zalo/connect")
    public void connectZalo(HttpServletResponse response) throws IOException {

        String verifier = PKCE.generateVerifier();
        String challenge = PKCE.generateChallenge(verifier);
        String url = zaloOAuthService.buildAuthorizeUrl();

        response.sendRedirect(url);
    }
}
