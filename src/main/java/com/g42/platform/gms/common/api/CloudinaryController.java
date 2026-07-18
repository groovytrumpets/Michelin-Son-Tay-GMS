package com.g42.platform.gms.common.api;

import com.g42.platform.gms.common.constant.FileUploadConstants;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Generic file upload controller using Cloudinary
 * Refactored to use ImageUploadService for code reusability
 */
@RestController
@RequestMapping("/home/uploads")
@RequiredArgsConstructor
public class CloudinaryController {

    private final ImageUploadService imageUploadService;
    
    /**
     * Upload file to Cloudinary
     * Default folder: garage/booking/
     */
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file) throws IOException {
        
        // Use ImageUploadService for validation and upload
        String url = imageUploadService.uploadImage(file, "garage/booking/");
        String publicId = imageUploadService.extractPublicId(url);
        
        Map<String, String> result = Map.of(
            "url", url,
            "publicId", publicId != null ? publicId : ""
        );

        return ResponseEntity.ok(ApiResponses.success(result));
    }

    /**
     * Upload ảnh cho chat nội bộ nhân viên — bổ sung path /image (giữ nguyên "/" ở trên
     * cho các nơi khác đang gọi, đây là mapping cộng thêm, không thay thế).
     * FE: src/services/chatUploadService.js -> uploadChatImage
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadChatImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        String url = imageUploadService.uploadImage(file, FileUploadConstants.FOLDER_CHAT_IMAGE);
        Map<String, Object> result = Map.of(
                "imageUrl", url,
                "publicId", Objects.toString(imageUploadService.extractPublicId(url), "")
        );
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    /**
     * Upload video cho chat nội bộ nhân viên.
     * FE: src/services/chatUploadService.js -> uploadChatVideo
     */
    @PostMapping(value = "/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadChatVideo(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponses.success(
                imageUploadService.uploadVideoDetailed(file, FileUploadConstants.FOLDER_CHAT_VIDEO)));
    }

    /**
     * Upload tệp bất kỳ cho chat nội bộ nhân viên.
     * FE: src/services/chatUploadService.js -> uploadChatFile
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadChatFile(
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(ApiResponses.success(
                imageUploadService.uploadFile(file, FileUploadConstants.FOLDER_CHAT_FILE)));
    }
}
