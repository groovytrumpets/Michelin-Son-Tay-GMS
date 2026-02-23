package com.g42.platform.gms.common.api;

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
}
