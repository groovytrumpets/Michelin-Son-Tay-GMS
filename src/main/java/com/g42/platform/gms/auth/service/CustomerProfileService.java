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
            profile.setEmail(normalizeEmail(request.getEmail(), customerId));
        }
        
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        
        if (request.getAvatar() != null) {
            profile.setAvatar(request.getAvatar());
        }

        if (request.getNotificationChannel() != null) {
            // Email đã được gán ở trên nên profile.getEmail() là giá trị sau cập nhật
            boolean canUseEmail = profile.getEmail() != null && !profile.getEmail().isBlank();
            if (request.getNotificationChannel() == com.g42.platform.gms.notification.domain.NotificationChannel.EMAIL && !canUseEmail) {
                throw new AuthException("Cần có email trước khi chọn nhận thông báo qua Email");
            }
            profile.setNotificationChannel(request.getNotificationChannel());
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
     * Chuẩn hoá email và chặn trùng — email là định danh đăng nhập thứ hai của khách hàng
     * (bên cạnh số điện thoại) nên không được để 2 tài khoản dùng chung.
     *
     * @return email đã trim, hoặc null nếu khách xoá email
     */
    private String normalizeEmail(String rawEmail, Integer customerId) {
        String email = rawEmail.trim();
        if (email.isEmpty()) {
            return null;
        }
        if (customerProfileRepository.existsByEmailIgnoreCaseAndCustomerIdNot(email, customerId)) {
            throw new AuthException("Email này đã được dùng cho tài khoản khác");
        }
        return email;
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

    /**
     * Switch customer role between INDIVIDUAL and DEALER
     * Only allowed if isDealer is true
     */
    @Transactional
    public CustomerProfile switchRole(Integer customerId) {
        CustomerProfile profile = findCustomerById(customerId);
        
        // If they are not a dealer, they cannot switch
        if (profile.getIsDealer() == null || !profile.getIsDealer()) {
            throw new AuthException("Tài khoản chưa được cấp phép đại lý");
        }
        
        // Toggle the role
        if (com.g42.platform.gms.customer.domain.enums.CustomerType.DEALER.equals(profile.getCustomerType())) {
            profile.setCustomerType(com.g42.platform.gms.customer.domain.enums.CustomerType.INDIVIDUAL);
        } else {
            profile.setCustomerType(com.g42.platform.gms.customer.domain.enums.CustomerType.DEALER);
        }
        
        CustomerProfile updated = customerProfileRepository.save(profile);
        log.info("Customer role switched: customerId={}, newRole={}", customerId, updated.getCustomerType());
        
        return updated;
    }
}
