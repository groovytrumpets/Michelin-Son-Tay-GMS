package com.g42.platform.gms.marketing.itempost.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.itempost.api.dto.ItemPostDtos;
import com.g42.platform.gms.marketing.itempost.app.PublicItemPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Danh mục và tag công khai — dùng cho thanh lọc ở trang bài viết phụ tùng. */
@RestController
@RequiredArgsConstructor
public class PublicItemPostTaxonomyController {

    private final PublicItemPostService publicItemPostService;

    @GetMapping("/api/public/item-post-categories")
    public ResponseEntity<ApiResponse<List<ItemPostDtos.CategoryDto>>> categories() {
        return ResponseEntity.ok(ApiResponses.success(publicItemPostService.activeCategories()));
    }

    @GetMapping("/api/public/item-post-tags")
    public ResponseEntity<ApiResponse<List<ItemPostDtos.TagDto>>> tags(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponses.success(publicItemPostService.popularTags(limit)));
    }
}
