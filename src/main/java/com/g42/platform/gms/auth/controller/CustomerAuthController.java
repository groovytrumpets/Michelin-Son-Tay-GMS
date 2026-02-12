package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.service.CustomerAuthService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/customer")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;

    // ============ NHÓM 1: CÓ TRẢ VỀ DATA ============

    /**
     * Trả về trạng thái user -> Dùng ApiResponse<CheckPhoneResponse>
     */
    @PostMapping("/check-status")
    public ResponseEntity<ApiResponse<CheckPhoneResponse>> checkStatus(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        CheckPhoneResponse result = customerAuthService.checkPhoneStatus(phone);
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    /**
     * Xác thực OTP xong trả về AuthResponse (để frontend biết) -> Dùng ApiResponse<AuthResponse>
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        AuthResponse response = customerAuthService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    /**
     * Login xong trả về Token -> Dùng ApiResponse<AuthResponse>
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest request) {
        AuthResponse response = customerAuthService.login(request);
        return ResponseEntity.ok(ApiResponses.success(response));
    }

    // ============ NHÓM 2: CHỈ TRẢ VỀ MESSAGE (DATA = VOID) ============

    /**
     * Gửi OTP -> Chỉ cần báo thành công -> Dùng ApiResponse<Void>
     */
    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        customerAuthService.requestOtp(phone);
        return ResponseEntity.ok(ApiResponses.successMessage("OTP has been sent to " + phone));
    }

    /**
     * Đặt PIN -> Chỉ cần báo thành công -> Dùng ApiResponse<Void>
     */
    @PostMapping("/setup-pin")
    public ResponseEntity<ApiResponse<Void>> setupPin(@RequestBody SetupPinRequest request) {
        customerAuthService.setupPin(request);
        return ResponseEntity.ok(ApiResponses.successMessage("PIN setup successfully. Please login."));
    }

    /**
     * Logout -> Chỉ cần báo thành công -> Dùng ApiResponse<Void>
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponses.successMessage("Logged out successfully"));
    }
}