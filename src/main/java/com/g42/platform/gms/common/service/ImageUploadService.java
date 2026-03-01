package com.g42.platform.gms.common.service;

import com.cloudinary.Cloudinary;
import com.g42.platform.gms.common.constant.FileUploadConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final Cloudinary cloudinary;

    /**
     * Upload avatar to Cloudinary
     * @param file Image file
     * @param folder Cloudinary folder (e.g., "garage/avatars/customer")
     * @return Secure URL of uploaded image
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        validateFile(file);
        
        // Simple upload without transformation - Cloudinary will store original
        // You can apply transformation via URL later if needed
        Map<String, Object> options = new HashMap<>();
        options.put("folder", folder);
        options.put("resource_type", "image");
        
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), options);
        String url = (String) uploadResult.get("secure_url");
        
        log.info("Image uploaded successfully: {}", url);
        return url;
    }

    /**
     * Upload customer avatar
     */
    public String uploadCustomerAvatar(MultipartFile file) throws IOException {
        return uploadImage(file, FileUploadConstants.FOLDER_CUSTOMER_AVATAR);
    }

    /**
     * Upload staff avatar
     */
    public String uploadStaffAvatar(MultipartFile file) throws IOException {
        return uploadImage(file, FileUploadConstants.FOLDER_STAFF_AVATAR);
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(FileUploadConstants.ERROR_FILE_EMPTY);
        }
        
        if (file.getSize() > FileUploadConstants.MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                String.format(FileUploadConstants.ERROR_FILE_TOO_LARGE, FileUploadConstants.MAX_IMAGE_SIZE_MB)
            );
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !FileUploadConstants.ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(FileUploadConstants.ERROR_INVALID_FILE_TYPE);
        }
    }

    /**
     * Delete image from Cloudinary by public_id
     */
    public void deleteImage(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        
        try {
            cloudinary.uploader().destroy(publicId, Map.of());
            log.info("Image deleted: {}", publicId);
        } catch (Exception e) {
            log.warn("Failed to delete image: {}", publicId, e);
        }
    }

    /**
     * Extract public_id from Cloudinary URL
     * Example: https://res.cloudinary.com/xxx/image/upload/v123/garage/avatars/customer/abc.jpg
     * Returns: garage/avatars/customer/abc
     */
    public String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }
        
        try {
            String[] parts = cloudinaryUrl.split("/upload/");
            if (parts.length < 2) {
                return null;
            }
            
            String afterUpload = parts[1];
            // Remove version (v123456789/)
            String withoutVersion = afterUpload.replaceFirst("v\\d+/", "");
            // Remove file extension
            int lastDot = withoutVersion.lastIndexOf('.');
            if (lastDot > 0) {
                return withoutVersion.substring(0, lastDot);
            }
            return withoutVersion;
        } catch (Exception e) {
            log.warn("Failed to extract public_id from URL: {}", cloudinaryUrl, e);
            return null;
        }
    }
}
