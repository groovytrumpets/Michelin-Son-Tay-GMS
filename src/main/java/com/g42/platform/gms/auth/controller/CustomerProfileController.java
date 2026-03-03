package com.g42.platform.gms.auth.controller;

import com.g42.platform.gms.auth.dto.CustomerProfileResponse;
import com.g42.platform.gms.auth.dto.UpdateCustomerProfileRequest;
import com.g42.platform.gms.auth.entity.CustomerPrincipal;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.mapper.CustomerProfileMapper;
import com.g42.platform.gms.auth.service.CustomerProfileService;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.service.ImageUploadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController {
    
    private final CustomerProfileService customerProfileService;
    private final CustomerProfileMapper customerProfileMapper;
    private final ImageUploadService imageUploadService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomerPrincipal principal) {
        
        CustomerProfile profile = customerProfileService.getProfile(principal.getCustomerId());
        CustomerProfileResponse response = customerProfileMapper.toResponse(profile);
        
        return ResponseEntity.ok(ApiResponses.success(response));
    }
    
    @PutMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @Valid @RequestBody UpdateCustomerProfileRequest request) {
        
        CustomerProfile updated = customerProfileService.updateProfile(
                principal.getCustomerId(), 
                request
        );
        CustomerProfileResponse response = customerProfileMapper.toResponse(updated);
        
        return ResponseEntity.ok(ApiResponses.success(response, "Cập nhật thông tin thành công"));
    }
    
    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadAvatar(
            @AuthenticationPrincipal CustomerPrincipal principal,
            @RequestParam("avatar") MultipartFile file) throws IOException {
        
        String avatarUrl = customerProfileService.updateAvatar(principal.getCustomerId(), file);
        
        return ResponseEntity.ok(ApiResponses.success(
            Map.of("avatarUrl", avatarUrl),
            "Cập nhật avatar thành công"
        ));
    }
}
