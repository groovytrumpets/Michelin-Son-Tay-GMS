package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.service.CustomerAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/customer")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    /**
     * REGISTER – Bước 1: Gửi OTP đăng ký mới (Anti-Spam)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        customerAuthService.register(request);
        return ResponseEntity.ok("OTP sent successfully");
    }

    /**
     * FORGOT PIN – Bước 1 (Phụ): Gửi OTP để mở khóa / Quên mật khẩu
     * Body JSON: { "phone": "0987654321" }
     */
    @PostMapping("/forgot-pin")
    public ResponseEntity<?> forgotPin(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        customerAuthService.forgotPin(phone);
        return ResponseEntity.ok("OTP for PIN reset sent successfully");
    }

    /**
     * VERIFY OTP – Bước 2: Xác thực OTP (Dùng chung cho cả Register và Forgot PIN)
     * Sau bước này, tài khoản sẽ được ACTIVE và Reset bộ đếm lỗi.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(
            @RequestBody VerifyOtpRequest request) {

        AuthResponse response = customerAuthService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    /**
     * SETUP PIN – Bước 3: Thiết lập mã PIN mới (Dùng chung cho cả Đăng ký mới và Đổi PIN sau khi quên)
     */
    @PostMapping("/setup-pin")
    public ResponseEntity<?> setupPin(@RequestBody SetupPinRequest request) {
        customerAuthService.setupPin(request);
        return ResponseEntity.ok("PIN set successfully");
    }

    /**
     * LOGIN – Bước 4: Đăng nhập và nhận JWT Token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest request) {

        AuthResponse response = customerAuthService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * LOGOUT – Mock (Client tự xóa Token)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Logged out");
    }
}