package com.g42.platform.gms.marketing.news.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.news.api.dto.PostDtos;
import com.g42.platform.gms.marketing.news.app.PublicPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Danh mục và tag công khai — dùng cho thanh lọc ở trang tin tức. */
@RestController
@RequiredArgsConstructor
public class PublicPostTaxonomyController {

    private final PublicPostService publicPostService;

    @GetMapping("/api/public/post-categories")
    public ResponseEntity<ApiResponse<List<PostDtos.CategoryDto>>> categories() {
        return ResponseEntity.ok(ApiResponses.success(publicPostService.activeCategories()));
    }

    @GetMapping("/api/public/post-tags")
    public ResponseEntity<ApiResponse<List<PostDtos.TagDto>>> tags(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponses.success(publicPostService.popularTags(limit)));
    }
}
