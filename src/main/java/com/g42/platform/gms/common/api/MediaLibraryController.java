package com.g42.platform.gms.common.api;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.service.MediaLibraryService;
import com.g42.platform.gms.common.service.MediaLibraryService.DeleteResult;
import com.g42.platform.gms.common.service.MediaLibraryService.MediaInUseException;
import com.g42.platform.gms.common.service.MediaLibraryService.MediaLibraryException;
import com.g42.platform.gms.common.service.MediaLibraryService.MediaNotFoundException;
import com.g42.platform.gms.common.service.MediaLibraryService.MediaPage;
import com.g42.platform.gms.common.service.MediaLibraryService.MediaUsage;
import com.g42.platform.gms.common.service.MediaLibraryService.UploadedAsset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/media-library")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCodes.MEDIA_VIEW + "')")
public class MediaLibraryController {

    private final MediaLibraryService mediaLibraryService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "60") int maxResults,
            @RequestParam(required = false) String prefix) {
        try {
            MediaPage page = mediaLibraryService.listImages(cursor, maxResults, prefix);
            return ResponseEntity.ok(ApiResponses.success(page));
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        } catch (MediaLibraryException exception) {
            return cloudinaryFailure(exception);
        }
    }

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<?>> usage(@RequestParam String publicId) {
        try {
            MediaUsage usage = mediaLibraryService.inspectUsage(publicId);
            return ResponseEntity.ok(ApiResponses.success(usage));
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        } catch (MediaNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponses.error("MEDIA_NOT_FOUND", exception.getMessage()));
        } catch (MediaLibraryException exception) {
            return cloudinaryFailure(exception);
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('" + PermissionCodes.MEDIA_UPLOAD + "')")
    public ResponseEntity<ApiResponse<?>> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(defaultValue = "garage/media-library") String folder) {
        try {
            List<UploadedAsset> uploaded = mediaLibraryService.uploadImages(files, folder);
            return ResponseEntity.ok(ApiResponses.success(uploaded, "Đã tải ảnh lên thư viện"));
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        } catch (MediaLibraryException exception) {
            return cloudinaryFailure(exception);
        }
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.MEDIA_DELETE + "')")
    public ResponseEntity<ApiResponse<?>> delete(@RequestBody DeleteMediaRequest request) {
        try {
            DeleteResult result = mediaLibraryService.deleteImage(request == null ? null : request.publicId());
            return ResponseEntity.ok(ApiResponses.success(result, "Đã xóa ảnh khỏi Cloudinary"));
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        } catch (MediaInUseException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponses.error("MEDIA_IN_USE", exception.getMessage()));
        } catch (MediaNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponses.error("MEDIA_NOT_FOUND", exception.getMessage()));
        } catch (MediaLibraryException exception) {
            return cloudinaryFailure(exception);
        }
    }

    private ResponseEntity<ApiResponse<?>> badRequest(String message) {
        return ResponseEntity.badRequest().body(ApiResponses.error("INVALID_MEDIA_REQUEST", message));
    }

    private ResponseEntity<ApiResponse<?>> cloudinaryFailure(MediaLibraryException exception) {
        log.error("Media library Cloudinary operation failed: {}", exception.getCode(), exception);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponses.error(exception.getCode(), exception.getMessage()));
    }

    public record DeleteMediaRequest(String publicId, Boolean invalidate) {}
}
