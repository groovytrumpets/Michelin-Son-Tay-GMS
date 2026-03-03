package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.dto.UpdateCustomerProfileRequest;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.common.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProfileService {
    
    private final CustomerProfileRepository customerProfileRepository;
    private final ImageUploadService imageUploadService;
    
    /**
     * Get customer profile by ID
     * @throws AuthException if customer not found
     */
    public CustomerProfile getProfile(Integer customerId) {
        return findCustomerById(customerId);
    }
    
    /**
     * Update customer profile (email, gender, avatar URL)
     */
    @Transactional
    public CustomerProfile updateProfile(Integer customerId, UpdateCustomerProfileRequest request) {
        CustomerProfile profile = findCustomerById(customerId);
        
        // Update only allowed fields
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail());
        }
        
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        
        if (request.getAvatar() != null) {
            profile.setAvatar(request.getAvatar());
        }
        
        CustomerProfile updated = customerProfileRepository.save(profile);
        log.info("Customer profile updated: customerId={}", customerId);
        
        return updated;
    }
    
    /**
     * Upload and update customer avatar
     * Automatically deletes old avatar if exists
     */
    @Transactional
    public String updateAvatar(Integer customerId, MultipartFile file) throws IOException {
        CustomerProfile profile = findCustomerById(customerId);
        
        // Delete old avatar if exists
        deleteOldAvatarIfExists(profile.getAvatar());
        
        // Upload new avatar
        String newAvatarUrl = imageUploadService.uploadCustomerAvatar(file);
        profile.setAvatar(newAvatarUrl);
        customerProfileRepository.save(profile);
        
        log.info("Customer avatar updated: customerId={}, url={}", customerId, newAvatarUrl);
        return newAvatarUrl;
    }
    
    /**
     * Find customer by ID or throw exception
     * Centralized method to avoid code duplication
     */
    private CustomerProfile findCustomerById(Integer customerId) {
        return customerProfileRepository.findById(customerId)
                .orElseThrow(() -> new AuthException("Không tìm thấy thông tin khách hàng"));
    }
    
    /**
     * Delete old avatar from Cloudinary if it's a Cloudinary URL
     */
    private void deleteOldAvatarIfExists(String oldAvatar) {
        if (oldAvatar == null || !oldAvatar.contains("cloudinary.com")) {
            return;
        }
        
        try {
            String publicId = imageUploadService.extractPublicId(oldAvatar);
            if (publicId != null) {
                imageUploadService.deleteImage(publicId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete old avatar: {}", oldAvatar, e);
        }
    }
}
