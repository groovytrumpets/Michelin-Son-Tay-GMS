package com.g42.platform.gms.marketing.itempost.api.controller;

import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.itempost.api.dto.ItemPostDtos;
import com.g42.platform.gms.marketing.itempost.app.ItemPostCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Quản lý danh mục bài viết phụ tùng. */
@RestController
@RequestMapping("/api/admin/item-post-categories")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + PermissionCodes.ITEM_POST_VIEW + "')")
public class AdminItemPostCategoryController {

    private final ItemPostCategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemPostDtos.CategoryDto>>> list() {
        return ResponseEntity.ok(ApiResponses.success(categoryService.listAll()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionCodes.ITEM_POST_EDIT + "')")
    public ResponseEntity<ApiResponse<ItemPostDtos.CategoryDto>> create(
            @Valid @RequestBody ItemPostDtos.CategorySaveRequest request) {
        return ResponseEntity.ok(ApiResponses.success(categoryService.create(request), "Đã tạo danh mục"));
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ITEM_POST_EDIT + "')")
    public ResponseEntity<ApiResponse<ItemPostDtos.CategoryDto>> update(
            @PathVariable Integer categoryId,
            @Valid @RequestBody ItemPostDtos.CategorySaveRequest request) {
        return ResponseEntity.ok(ApiResponses.success(
                categoryService.update(categoryId, request), "Đã cập nhật danh mục"));
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasAuthority('" + PermissionCodes.ITEM_POST_EDIT + "')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer categoryId) {
        categoryService.delete(categoryId);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá danh mục"));
    }
}
