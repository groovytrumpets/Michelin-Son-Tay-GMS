package com.g42.platform.gms.marketing.news.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.news.api.dto.PostDtos;
import com.g42.platform.gms.marketing.news.app.PostViewService;
import com.g42.platform.gms.marketing.news.app.PublicPostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API tin tức cho khách — không cần đăng nhập. */
@RestController
@RequestMapping("/api/public/posts")
@RequiredArgsConstructor
public class PublicPostController {

    private static final int MAX_PAGE_SIZE = 50;

    private final PublicPostService publicPostService;
    private final PostViewService postViewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostDtos.SummaryDto>>> list(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String tagSlug,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Page<PostDtos.SummaryDto> result = publicPostService.list(
                categorySlug, tagSlug, q, featured, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<PostDtos.DetailDto>> detail(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponses.success(publicPostService.getBySlug(slug)));
    }

    @GetMapping("/{slug}/related")
    public ResponseEntity<ApiResponse<List<PostDtos.SummaryDto>>> related(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponses.success(publicPostService.related(slug)));
    }

    /** Ghi nhận lượt xem; trả về {@code counted=false} khi bị coi là xem lặp. */
    @PostMapping("/{slug}/view")
    public ResponseEntity<ApiResponse<PostDtos.ViewResultDto>> registerView(
            @PathVariable String slug,
            @RequestBody(required = false) PostDtos.ViewRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponses.success(postViewService.registerView(slug, body, request)));
    }

    @PostMapping("/{slug}/share")
    public ResponseEntity<ApiResponse<Void>> registerShare(@PathVariable String slug) {
        publicPostService.registerShare(slug);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã ghi nhận lượt chia sẻ"));
    }
}
