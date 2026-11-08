package com.g42.platform.gms.notification.api.controller;

import com.g42.platform.gms.notification.application.service.NotificationService;
import com.g42.platform.gms.notification.infrastructure.ZaloOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * /zalo/otp, /zalo/booking-confirmed, /zalo/feedback: endpoint thử gửi tin (chỉ nhân viên — SecurityConfig).
 * /zalo/login, /zalo/callback: công khai, là hai đầu của luồng OAuth OA. Màn /zalo-oa-config
 * bắt đầu luồng qua /api/admin/zalo-oa/oauth/authorize-url (có kèm trang quay về), còn /zalo/login
 * giữ lại cho ai quen gõ thẳng.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/zalo")
public class NotificationTestController {
    private final NotificationService notificationSender;
    private final ZaloOAuthService zaloOAuthService;

    @PostMapping("/booking-confirmed")
    public String bookingConfirmed(){
        List<String> strings = List.of(new String[]{"a", "b"});
        notificationSender.sendBookingCf(
                "0386037357",
                "NGUYEN VAN A",
                strings,"MTS-12",LocalDateTime.now(),"Here"
        );
        return "OK";
    }
    @PostMapping("/otp")
    public String sendOtp(){
        notificationSender.sendOtpViaZalo("0386037357","123456");
        return "OK";
    }
    @PostMapping("/feedback")
    public String sendFeedback(){
        notificationSender.sendFeedbackViaZalo("0386037357","Nguyen Van A","123456");
        return "OK";
    }

    @GetMapping("/login")
    public void connectZalo(HttpServletResponse response) throws IOException {
        response.sendRedirect(zaloOAuthService.buildAuthorizeUrl(null, null));
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam(required = false) String code,
                                           @RequestParam(required = false) String state,
                                           HttpServletResponse response) throws IOException {
        ZaloOAuthService.CallbackResult result = zaloOAuthService.handleCallback(code, state);
        if (result.returnUrl() != null) {
            String target = UriComponentsBuilder.fromUriString(result.returnUrl())
                    .replaceQueryParam("zalo_connected", result.success() ? "1" : "0")
                    .replaceQueryParam("zalo_message", result.message())
                    .encode()
                    .toUriString();
            response.sendRedirect(target);
            return null;
        }
        return ResponseEntity.status(result.success() ? 200 : 400)
                .contentType(new MediaType("text", "plain", java.nio.charset.StandardCharsets.UTF_8))
                .body(result.message());
    }
}
