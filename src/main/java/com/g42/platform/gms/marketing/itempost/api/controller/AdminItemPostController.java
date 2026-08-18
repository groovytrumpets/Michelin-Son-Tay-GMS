package com.g42.platform.gms.marketing.itempost.api.controller;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.marketing.itempost.api.dto.ItemPostDtos;
import com.g42.platform.gms.marketing.itempost.app.ItemPostService;
import com.g42.platform.gms.marketing.itempost.domain.ItemPostStatus;
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
import java.util.List;
import java.util.Map;

/** Soạn thảo và duyệt bài viết phụ tùng. */
@Slf4j
@RestController
@RequestMapping("/api/admin/item-posts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'RECEPTIONIST')")
public class AdminItemPostController {

    private static final String IMAGE_FOLDER = "garage/item-posts";

    private final ItemPostService itemPostService;
    private final ImageUploadService imageUploadService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ItemPostDtos.SummaryDto>>> search(
            @RequestParam(required = false) ItemPostStatus status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String tagSlug,
            @RequestParam(required = false) Integer catalogItemId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt", "itemPostId"));
        return ResponseEntity.ok(ApiResponses.success(
                itemPostService.search(status, categoryId, tagSlug, catalogItemId, q, pageable)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ItemPostDtos.StatsDto>> stats() {
        return ResponseEntity.ok(ApiResponses.success(itemPostService.stats()));
    }

    /** Bài viết gắn với một phụ tùng — dùng ở /part-management/blog/:itemId. */
    @GetMapping("/by-catalog-item/{catalogItemId}")
    public ResponseEntity<ApiResponse<List<ItemPostDtos.AdminDetailDto>>> byCatalogItem(
            @PathVariable Integer catalogItemId) {
        return ResponseEntity.ok(ApiResponses.success(itemPostService.getByCatalogItem(catalogItemId)));
    }

    @GetMapping("/{itemPostId}")
    public ResponseEntity<ApiResponse<ItemPostDtos.AdminDetailDto>> detail(@PathVariable Long itemPostId) {
        return ResponseEntity.ok(ApiResponses.success(itemPostService.getForEdit(itemPostId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ItemPostDtos.AdminDetailDto>> create(
            @Valid @RequestBody ItemPostDtos.SaveRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal == null ? null : principal.getStaffId();
        return ResponseEntity.ok(ApiResponses.success(
                itemPostService.create(request, staffId), "Đã lưu bài viết"));
    }

    @PutMapping("/{itemPostId}")
    public ResponseEntity<ApiResponse<ItemPostDtos.AdminDetailDto>> update(
            @PathVariable Long itemPostId,
            @Valid @RequestBody ItemPostDtos.SaveRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                itemPostService.update(itemPostId, request), "Đã cập nhật bài viết"));
    }

    /** Duyệt, trả lại, hẹn giờ hoặc lưu trữ bài. */
    @PatchMapping("/{itemPostId}/status")
    public ResponseEntity<ApiResponse<ItemPostDtos.AdminDetailDto>> changeStatus(
            @PathVariable Long itemPostId,
            @RequestBody ItemPostDtos.StatusChangeRequest request,
            @AuthenticationPrincipal StaffPrincipal principal) {
        Integer staffId = principal == null ? null : principal.getStaffId();
        return ResponseEntity.ok(ApiResponses.success(
                itemPostService.changeStatus(itemPostId, request, staffId), "Đã cập nhật trạng thái"));
    }

    @DeleteMapping("/{itemPostId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long itemPostId) {
        itemPostService.delete(itemPostId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá bài viết"));
    }

    /**
     * Tạo item_post PUBLISHED cho mọi catalog item công khai chưa có bài viết — chạy
     * tay một lần sau khi deploy tính năng URL slug để mọi id số cũ có slug tương ứng.
     * Idempotent: gọi lại nhiều lần chỉ tạo thêm cho những item mới xuất hiện.
     */
    @PostMapping("/backfill")
    public ResponseEntity<ApiResponse<ItemPostDtos.BackfillResultDto>> backfill() {
        return ResponseEntity.ok(ApiResponses.success(
                itemPostService.backfillFromCatalogItems(), "Đã backfill bài viết cho catalog item"));
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
            log.error("Tải ảnh bài viết phụ tùng lên Cloudinary thất bại", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponses.error("UPLOAD_FAILED", "Không tải được ảnh, vui lòng thử lại"));
        }
    }
}
