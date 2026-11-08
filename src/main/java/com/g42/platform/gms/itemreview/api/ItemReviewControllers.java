package com.g42.platform.gms.itemreview.api;

import com.g42.platform.gms.auth.entity.CustomerPrincipal;
import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.authz.PermissionCodes;
import com.g42.platform.gms.common.dto.ApiResponse;
import com.g42.platform.gms.common.dto.ApiResponses;
import com.g42.platform.gms.itemreview.api.ItemReviewDtos.*;
import com.g42.platform.gms.itemreview.app.ItemReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Ba nhóm endpoint của đánh giá sản phẩm, tách theo tầng bảo vệ ở SecurityConfig:
 * /api/public/** (permitAll), /api/customer/** (token khách), /api/admin/** (chỉ nhân viên + mã quyền).
 */
public final class ItemReviewControllers {
    private ItemReviewControllers() {}

    /** Ai cũng đọc được — trang chi tiết phụ tùng/dịch vụ, thẻ sản phẩm ở trang danh sách. */
    @RestController
    @RequestMapping("/api/public/item-reviews")
    @RequiredArgsConstructor
    public static class PublicItemReviewController {
        private final ItemReviewService service;

        @GetMapping("/{catalogItemId}")
        public ResponseEntity<ApiResponse<PublicReviewsResponse>> list(@PathVariable int catalogItemId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "5") int size,
                                                                       @RequestParam(required = false) Integer rating,
                                                                       @RequestParam(required = false) String sort) {
            return ResponseEntity.ok(ApiResponses.success(service.publicReviews(catalogItemId, page, size, rating, sort)));
        }

        /** ids=1,2,3 (tối đa 200). Mặt hàng chưa có đánh giá nào thì không có trong kết quả. */
        @GetMapping("/summaries")
        public ResponseEntity<ApiResponse<List<BriefSummary>>> summaries(@RequestParam String ids) {
            List<Integer> parsed = Arrays.stream(ids.split(","))
                    .map(String::trim)
                    .filter(s -> s.matches("\\d{1,10}"))
                    .map(Integer::valueOf)
                    .filter(Objects::nonNull)
                    .toList();
            return ResponseEntity.ok(ApiResponses.success(service.briefSummaries(parsed)));
        }
    }

    /** Khách đăng nhập gửi / sửa / xoá đánh giá của chính mình. */
    @RestController
    @RequestMapping("/api/customer/item-reviews")
    @RequiredArgsConstructor
    public static class CustomerItemReviewController {
        private final ItemReviewService service;

        @GetMapping("/{catalogItemId}/mine")
        public ResponseEntity<ApiResponse<MyReviewResponse>> mine(@PathVariable int catalogItemId,
                                                                  @AuthenticationPrincipal Object principal) {
            return ResponseEntity.ok(ApiResponses.success(service.myReview(catalogItemId, customerId(principal))));
        }

        @PutMapping("/{catalogItemId}")
        public ResponseEntity<ApiResponse<MyReview>> upsert(@PathVariable int catalogItemId,
                                                            @RequestBody UpsertRequest req,
                                                            @AuthenticationPrincipal Object principal) {
            MyReview saved = service.upsert(catalogItemId, customerId(principal), req);
            return ResponseEntity.ok(ApiResponses.success(saved, "Cảm ơn bạn đã đánh giá!"));
        }

        @DeleteMapping("/{catalogItemId}")
        public ResponseEntity<ApiResponse<Void>> delete(@PathVariable int catalogItemId,
                                                        @AuthenticationPrincipal Object principal) {
            service.deleteMine(catalogItemId, customerId(principal));
            return ResponseEntity.ok(ApiResponses.successMessage("Đã xoá đánh giá"));
        }

        /** Token nhân viên cũng "authenticated" — nhưng nhân viên không được đánh giá thay khách. */
        private static int customerId(Object principal) {
            if (principal instanceof CustomerPrincipal c && c.getCustomerId() != null) {
                return c.getCustomerId();
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vui lòng đăng nhập tài khoản khách hàng để đánh giá.");
        }
    }

    /** Màn /item-review-management. */
    @RestController
    @RequestMapping("/api/admin/item-reviews")
    @RequiredArgsConstructor
    public static class AdminItemReviewController {
        private final ItemReviewService service;

        @GetMapping
        @PreAuthorize("hasAuthority('" + PermissionCodes.ITEM_REVIEW_VIEW + "')")
        public ResponseEntity<ApiResponse<AdminListResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "20") int size,
                                                                   @RequestParam(required = false) Integer rating,
                                                                   @RequestParam(required = false) String status,
                                                                   @RequestParam(required = false) Integer catalogItemId,
                                                                   @RequestParam(required = false) String itemType,
                                                                   @RequestParam(required = false) Boolean unreplied,
                                                                   @RequestParam(required = false) String search) {
            return ResponseEntity.ok(ApiResponses.success(
                    service.adminList(page, size, rating, status, catalogItemId, itemType, unreplied, search)));
        }

        @PatchMapping("/{reviewId}/status")
        @PreAuthorize("hasAuthority('" + PermissionCodes.ITEM_REVIEW_EDIT + "')")
        public ResponseEntity<ApiResponse<Void>> setStatus(@PathVariable int reviewId, @RequestBody StatusRequest req) {
            service.setStatus(reviewId, req.status(), req.reason());
            return ResponseEntity.ok(ApiResponses.successMessage("Đã cập nhật trạng thái đánh giá"));
        }

        @PutMapping("/{reviewId}/reply")
        @PreAuthorize("hasAuthority('" + PermissionCodes.ITEM_REVIEW_EDIT + "')")
        public ResponseEntity<ApiResponse<Void>> reply(@PathVariable int reviewId, @RequestBody ReplyRequest req,
                                                       @AuthenticationPrincipal StaffPrincipal principal) {
            service.reply(reviewId, req.reply(), principal.getStaffId());
            return ResponseEntity.ok(ApiResponses.successMessage("Đã lưu phản hồi"));
        }
    }
}
