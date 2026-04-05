package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.service.StaffAuthService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
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

    @PostMapping("/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestOtp(@RequestBody LoginRequest phone) {
        staffAuthService.requestOtpPhone(phone.getPhone());
        return ResponseEntity.ok(ApiResponses.successMessage("OTP has been sent to " + phone));
    }
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(@RequestBody VerifyOtpRequest request) {
        AuthResponse response = staffAuthService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponses.success(response));
    }

}
