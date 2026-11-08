package com.g42.platform.gms.itemreview.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ItemReviewDtos {
    private ItemReviewDtos() {}

    /** distribution: số sao (1..5) -> số đánh giá. Chỉ tính đánh giá đang hiện. */
    public record Summary(int catalogItemId, double average, int count, int verifiedCount, Map<Integer, Integer> distribution) {}

    /** Bản rút gọn cho thẻ sản phẩm ở trang danh sách. */
    public record BriefSummary(int catalogItemId, double average, int count) {}

    /** Đánh giá hiện cho mọi người xem — tên khách đã che bớt. */
    public record PublicReview(int reviewId, int rating, String comment, String customerName, boolean verifiedPurchase,
                               Instant createdAt, Instant updatedAt, String staffReply, Instant repliedAt) {}

    public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}

    public record PublicReviewsResponse(Summary summary, PageResult<PublicReview> reviews) {}

    /** Đánh giá của chính khách đang đăng nhập (kể cả khi bị ẩn, để họ biết). */
    public record MyReview(Integer reviewId, Integer rating, String comment, String status, String hiddenReason,
                           boolean verifiedPurchase, Instant createdAt, Instant updatedAt, String staffReply) {}

    public record MyReviewResponse(MyReview review, boolean verifiedPurchase) {}

    public record UpsertRequest(Integer rating, String comment) {}

    public record AdminReview(int reviewId, int catalogItemId, String itemName, String itemType, String itemSlug,
                              int customerId, String customerName, String customerPhone, int rating, String comment,
                              boolean verifiedPurchase, String status, String hiddenReason, String staffReply,
                              Integer repliedByStaffId, Instant repliedAt, Instant createdAt, Instant updatedAt) {}

    public record AdminStats(long total, double average, long hidden, long unreplied, long lowRating, long last30Days) {}

    public record AdminListResponse(AdminStats stats, PageResult<AdminReview> reviews) {}

    public record StatusRequest(String status, String reason) {}

    public record ReplyRequest(String reply) {}
}
