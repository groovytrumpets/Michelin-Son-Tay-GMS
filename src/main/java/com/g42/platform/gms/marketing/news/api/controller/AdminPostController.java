package com.g42.platform.gms.marketing.news.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.marketing.news.api.dto.PostDtos;
import com.g42.platform.gms.marketing.news.app.PostService;
import com.g42.platform.gms.marketing.news.domain.PostStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/** Soạn thảo và duyệt bài viết. */
@Slf4j
@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'RECEPTIONIST')")
public class AdminPostController {

    private static final String IMAGE_FOLDER = "garage/posts";

    private final PostService postService;
    private final ImageUploadService imageUploadService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostDtos.SummaryDto>>> search(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String tagSlug,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt", "postId"));
        return ResponseEntity.ok(ApiResponses.success(
                postService.search(status, categoryId, tagSlug, q, pageable)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PostDtos.StatsDto>> stats() {
        return ResponseEntity.ok(ApiResponses.success(postService.stats()));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDtos.AdminDetailDto>> detail(@PathVariable Long postId) {
        return ResponseEntity.ok(ApiResponses.success(postService.getForEdit(postId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostDtos.AdminDetailDto>> create(
            @Valid @RequestBody PostDtos.SaveRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal == null ? null : principal.getStaffId();
        return ResponseEntity.ok(ApiResponses.success(
                postService.create(request, staffId), "Đã lưu bài viết"));
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDtos.AdminDetailDto>> update(
            @PathVariable Long postId,
            @Valid @RequestBody PostDtos.SaveRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                postService.update(postId, request), "Đã cập nhật bài viết"));
    }

    /** Duyệt, trả lại, hẹn giờ hoặc lưu trữ bài. */
    @PatchMapping("/{postId}/status")
    public ResponseEntity<ApiResponse<PostDtos.AdminDetailDto>> changeStatus(
            @PathVariable Long postId,
            @RequestBody PostDtos.StatusChangeRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal == null ? null : principal.getStaffId();
        return ResponseEntity.ok(ApiResponses.success(
                postService.changeStatus(postId, request, staffId), "Đã cập nhật trạng thái"));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long postId) {
        postService.softDelete(postId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá bài viết"));
    }

    /** Ảnh chèn trong bài và ảnh bìa — đẩy thẳng lên Cloudinary như các phân hệ khác. */
    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = imageUploadService.uploadImage(file, IMAGE_FOLDER);
            return ResponseEntity.ok(ApiResponses.success(Map.of("url", url)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponses.error("INVALID_FILE", e.getMessage()));
        } catch (IOException e) {
            log.error("Tải ảnh bài viết lên Cloudinary thất bại", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponses.error("UPLOAD_FAILED", "Không tải được ảnh, vui lòng thử lại"));
        }
    }
}
