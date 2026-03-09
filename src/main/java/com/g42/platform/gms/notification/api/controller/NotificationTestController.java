package com.g42.platform.gms.notification.api.controller;

import com.g42.platform.gms.notification.api.dto.TokenRes;
import com.g42.platform.gms.notification.application.service.NotificationService;
import com.g42.platform.gms.notification.domain.NotificationSender;
import com.g42.platform.gms.notification.infrastructure.PKCE;
import com.g42.platform.gms.notification.infrastructure.ZaloOAuthService;
import com.g42.platform.gms.notification.infrastructure.entity.ZaloToken;
import com.g42.platform.gms.notification.infrastructure.repository.ZaloTokenRepo;
import com.nimbusds.oauth2.sdk.TokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;

@RestController
@AllArgsConstructor
@RequestMapping("/zalo")
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
    @GetMapping("/login")
    public void connectZalo(HttpServletResponse response) throws IOException {

        String verifier = PKCE.generateVerifier();
        String challenge = PKCE.generateChallenge(verifier);
        String url = zaloOAuthService.buildAuthorizeUrl();

        response.sendRedirect(url);
    }
    private ZaloTokenRepo zaloTokenRepo;
    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code,
                                           @RequestParam String state){
        //todo: find record
        ZaloToken zaloToken = zaloTokenRepo.findByState(state);
        if (zaloToken == null) {
            return ResponseEntity.badRequest().body("Invalid state");
        }
        String codeVerifier = zaloToken.getCodeVerifier();
        //todo: call zalo api to save token
        TokenRes token = zaloOAuthService.getAccessToken(code, codeVerifier);
        //todo: save token to db
        zaloToken.setAccessToken(token.getAccess_token());
        zaloToken.setRefreshToken(token.getRefresh_token());
        zaloTokenRepo.save(zaloToken);
        return ResponseEntity.ok("Zalo connected successfully");
    }
}
