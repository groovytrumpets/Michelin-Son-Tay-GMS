package com.g42.platform.gms.common.constant;

import java.util.List;

/**
 * Constants for file upload validation
 * Centralized to avoid duplication across services
 */
public final class FileUploadConstants {
    
    private FileUploadConstants() {
        // Prevent instantiation
    }
    
    // File size limits
    public static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    public static final int MAX_IMAGE_SIZE_MB = 5;
    
    // Allowed image types
    public static final List<String> ALLOWED_IMAGE_TYPES = List.of(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp"
    );
    
    // Cloudinary folders
    public static final String FOLDER_CUSTOMER_AVATAR = "garage/avatars/customer";
    public static final String FOLDER_STAFF_AVATAR = "garage/avatars/staff";
    public static final String FOLDER_BOOKING = "garage/booking";
    public static final String FOLDER_VEHICLE = "garage/vehicles";
    
    // Image transformation
    public static final int AVATAR_SIZE = 500; // 500x500 pixels
    public static final int IMAGE_QUALITY = 80; // 1-100, 80 = good quality
    
    // Error messages
    public static final String ERROR_FILE_EMPTY = "File không được để trống";
    public static final String ERROR_FILE_TOO_LARGE = "File quá lớn. Kích thước tối đa: %d MB";
    public static final String ERROR_INVALID_FILE_TYPE = "Định dạng file không hợp lệ. Chỉ chấp nhận: JPEG, PNG, WEBP";
}
