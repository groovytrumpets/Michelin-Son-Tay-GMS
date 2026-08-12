package com.g42.platform.gms.marketing.navmenu.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.common.service.ImageUploadService;
import com.g42.platform.gms.marketing.navmenu.api.dto.NavMenuItemDto;
import com.g42.platform.gms.marketing.navmenu.app.NavMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Cấu hình menu điều hướng. */
@Slf4j
@RestController
@RequestMapping("/api/admin/nav-menu")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class AdminNavMenuController {

    private static final String IMAGE_FOLDER = "garage/nav-menu";

    private final NavMenuService navMenuService;
    private final ImageUploadService imageUploadService;

    @GetMapping("/{locationCode}")
    public ResponseEntity<ApiResponse<List<NavMenuItemDto.ItemDto>>> getMenu(@PathVariable String locationCode) {
        return ResponseEntity.ok(ApiResponses.success(navMenuService.getAdminTree(locationCode)));
    }

    @GetMapping("/item-types")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> itemTypes() {
        return ResponseEntity.ok(ApiResponses.success(navMenuService.availableItemTypes()));
    }

    @PostMapping("/{locationCode}")
    public ResponseEntity<ApiResponse<NavMenuItemDto.ItemDto>> create(
            @PathVariable String locationCode,
            @Valid @RequestBody NavMenuItemDto.SaveRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                navMenuService.create(locationCode, request), "Đã thêm mục menu"));
    }

    @PutMapping("/items/{navItemId}")
    public ResponseEntity<ApiResponse<NavMenuItemDto.ItemDto>> update(
            @PathVariable Integer navItemId,
            @Valid @RequestBody NavMenuItemDto.SaveRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                navMenuService.update(navItemId, request), "Đã cập nhật mục menu"));
    }

    /** Áp kết quả kéo thả cho cả cây trong một lần gọi. */
    @PutMapping("/{locationCode}/reorder")
    public ResponseEntity<ApiResponse<List<NavMenuItemDto.ItemDto>>> reorder(
            @PathVariable String locationCode,
            @RequestBody NavMenuItemDto.ReorderRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                navMenuService.reorder(locationCode, request), "Đã lưu thứ tự menu"));
    }

    @DeleteMapping("/items/{navItemId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer navItemId) {
        navMenuService.delete(navItemId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá mục menu"));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(ApiResponses.success(
                    Map.of("url", imageUploadService.uploadImage(file, IMAGE_FOLDER))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponses.error("INVALID_FILE", e.getMessage()));
        } catch (IOException e) {
            log.error("Tải ảnh menu lên Cloudinary thất bại", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponses.error("UPLOAD_FAILED", "Không tải được ảnh, vui lòng thử lại"));
        }
    }
}
