package com.g42.platform.gms.marketing.itempost.api.controller;

import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.marketing.itempost.api.dto.ItemPostDtos;
import com.g42.platform.gms.marketing.itempost.app.ItemPostViewService;
import com.g42.platform.gms.marketing.itempost.app.PublicItemPostService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API bài viết phụ tùng cho khách — không cần đăng nhập. */
@RestController
@RequestMapping("/api/public/item-posts")
@RequiredArgsConstructor
public class PublicItemPostController {

    private static final int MAX_PAGE_SIZE = 50;

    private final PublicItemPostService publicItemPostService;
    private final ItemPostViewService itemPostViewService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ItemPostDtos.SummaryDto>>> list(
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String tagSlug,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Integer catalogItemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Page<ItemPostDtos.SummaryDto> result = publicItemPostService.list(
                categorySlug, tagSlug, q, featured, catalogItemId, PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(ApiResponses.success(result));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ItemPostDtos.DetailDto>> detail(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponses.success(publicItemPostService.getBySlug(slug)));
    }

    /**
     * Tra bài viết đã đăng theo catalogItemId — FE dùng để chuyển hướng URL id số cũ
     * ({@code /services/1}) sang URL slug mới ({@code /services/ten-dich-vu}).
     */
    @GetMapping("/by-catalog-item/{catalogItemId}")
    public ResponseEntity<ApiResponse<ItemPostDtos.DetailDto>> byCatalogItem(@PathVariable Integer catalogItemId) {
        return ResponseEntity.ok(ApiResponses.success(publicItemPostService.getByCatalogItemId(catalogItemId)));
    }

    @GetMapping("/{slug}/related")
    public ResponseEntity<ApiResponse<List<ItemPostDtos.SummaryDto>>> related(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponses.success(publicItemPostService.related(slug)));
    }

    /** Ghi nhận lượt xem; trả về {@code counted=false} khi bị coi là xem lặp. */
    @PostMapping("/{slug}/view")
    public ResponseEntity<ApiResponse<ItemPostDtos.ViewResultDto>> registerView(
            @PathVariable String slug,
            @RequestBody(required = false) ItemPostDtos.ViewRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponses.success(itemPostViewService.registerView(slug, body, request)));
    }

    @PostMapping("/{slug}/share")
    public ResponseEntity<ApiResponse<Void>> registerShare(@PathVariable String slug) {
        publicItemPostService.registerShare(slug);
        return ResponseEntity.ok(ApiResponses.successMessage("Đã ghi nhận lượt chia sẻ"));
    }
}
