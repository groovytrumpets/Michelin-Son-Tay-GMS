package com.g42.platform.gms.common.api;

import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController

@RequestMapping("/home/uploads")
public class CloudinaryController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png");
    @Autowired
    Cloudinary cloudinary;
    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
    if (file.getSize() > MAX_FILE_SIZE) {
        return ResponseEntity.badRequest().body("File is too large");
    }
    if (file.getContentType() == null || !ALLOWED_TYPES.contains(file.getContentType())) {
        return ResponseEntity.badRequest().body("Invalid file type");
    }
        Map<String, Object> options = Map.of(
                "folder", "garage/booking/",
                "resource_type", "image",
                "use_filename", true,
                "unique_filename", true
        );
    Map uploadResult = cloudinary.uploader().upload(file.getBytes(),options);

        String url = (String) uploadResult.get("secure_url");
        String publicId = (String) uploadResult.get("public_id");
        System.err.println("URL: " + url);
        System.err.println("publicId: " + publicId);

        return ResponseEntity.ok(Map.of(
                "url", url,
                "publicId", publicId
        ));
    }
}
