package com.g42.platform.gms.marketing.news.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.news.api.dto.PostDtos;
import com.g42.platform.gms.marketing.news.app.PostCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Quản lý danh mục tin tức. */
@RestController
@RequestMapping("/api/admin/post-categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCodes.POST_VIEW + "')")
public class AdminPostCategoryController {

    private final PostCategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PostDtos.CategoryDto>>> list() {
        return ResponseEntity.ok(ApiResponses.success(categoryService.listAll()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.POST_EDIT + "')")
    public ResponseEntity<ApiResponse<PostDtos.CategoryDto>> create(
            @Valid @RequestBody PostDtos.CategorySaveRequest request) {
        return ResponseEntity.ok(ApiResponses.success(categoryService.create(request), "Đã tạo danh mục"));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.POST_EDIT + "')")
    public ResponseEntity<ApiResponse<PostDtos.CategoryDto>> update(
            @PathVariable Integer categoryId,
            @Valid @RequestBody PostDtos.CategorySaveRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                categoryService.update(categoryId, request), "Đã cập nhật danh mục"));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.POST_EDIT + "')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer categoryId) {
        categoryService.delete(categoryId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá danh mục"));
    }
}
