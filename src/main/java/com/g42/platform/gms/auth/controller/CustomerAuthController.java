package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.service.CustomerAuthService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.notification.domain.NotificationChannel;
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
        CheckPhoneResponse result = customerAuthService.checkPhoneStatus(readIdentifier(body));
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
    public ResponseEntity<ApiResponse<OtpSentResponse>> requestOtp(@RequestBody Map<String, String> body) {
        NotificationChannel sentVia = customerAuthService.requestOtp(readIdentifier(body), readChannel(body));
        return ResponseEntity.ok(ApiResponses.success(new OtpSentResponse(sentVia)));
    }

    /**
     * Định danh đăng nhập là số điện thoại HOẶC email.
     * Vẫn đọc khoá "phone" để tương thích client cũ, chấp nhận thêm "identifier".
     */
    private String readIdentifier(Map<String, String> body) {
        String identifier = body.get("identifier");
        return (identifier != null && !identifier.isBlank()) ? identifier : body.get("phone");
    }

    /**
     * Kênh nhận mã do khách chọn ở màn quên mật khẩu ("ZALO" / "EMAIL").
     * Không gửi hoặc gửi giá trị lạ thì trả null để hệ thống tự quyết kênh.
     */
    private NotificationChannel readChannel(Map<String, String> body) {
        String channel = body.get("channel");
        if (channel == null || channel.isBlank()) {
            return null;
        }
        try {
            return NotificationChannel.valueOf(channel.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
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