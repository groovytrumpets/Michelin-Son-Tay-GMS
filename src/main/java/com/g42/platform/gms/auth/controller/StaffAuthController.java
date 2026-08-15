package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.service.StaffAuthService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.notification.domain.NotificationChannel;

import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/auth/staff-auth")
public class StaffAuthController {
    @Autowired
    private final StaffAuthService staffAuthService;
    private final AuthenticationManager authenticationManager;



//    @GetMapping
//    public Iterable<StaffAuthDto> getAllStaffAuth(){
//        return staffAuthService.getAllStaffAuth();
//    }

//    @GetMapping("/{id}")
//    public ResponseEntity<StaffAuthDto> getStaftAuthById(@PathVariable int id){
//        return staffAuthService.getStaffAuthById(id);
//    }
//    @PostMapping("/login")
//    public String login(@RequestBody LoginRequest loginRequest){
////        System.out.println("PHONE = " + loginRequest.getPhone());
////        System.out.println("PIN   = " + loginRequest.getPin());
//
//        return staffAuthService.verifyStaffAuth(loginRequest);
//    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<StaffAuthResponse>> login(@RequestBody LoginRequest loginRequest){
//        System.out.println("PHONE = " + loginRequest.getPhone());
//        System.out.println("PIN   = " + loginRequest.getPin());
        StaffAuthResponse authResponse = staffAuthService.verifyStaffAuth(loginRequest);

        return ResponseEntity.ok(ApiResponses.success(authResponse));
    }

    /** Kênh nhận mã khả dụng của tài khoản, cho màn quên mật khẩu hiện lựa chọn phù hợp. */
    @PostMapping("/check-contact")
    public ResponseEntity<ApiResponse<ContactChannelsResponse>> checkContact(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponses.success(
                staffAuthService.getContactChannels(readIdentifier(body))));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<OtpSentResponse>> requestOtp(@RequestBody Map<String, String> body) {
        NotificationChannel sentVia = staffAuthService.requestOtpPhone(readIdentifier(body), readChannel(body));
        return ResponseEntity.ok(ApiResponses.success(new OtpSentResponse(sentVia)));
    }

    /** Định danh là số điện thoại HOẶC email; giữ khoá "phone" để tương thích client cũ. */
    private String readIdentifier(Map<String, String> body) {
        String identifier = body.get("identifier");
        return (identifier != null && !identifier.isBlank()) ? identifier : body.get("phone");
    }

    /** Kênh nhân viên chọn ở màn quên mật khẩu; giá trị lạ hoặc trống thì để hệ thống tự quyết. */
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
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        AuthResponse response = staffAuthService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponses.success(response));
    }
    @PostMapping("/setup-pass")
    public ResponseEntity<ApiResponse<Void>> setupPin(@RequestBody SetupPinRequest request) {
        staffAuthService.setupPassword(request);
        return ResponseEntity.ok(ApiResponses.successMessage("PIN setup successfully. Please login."));
    }

}
