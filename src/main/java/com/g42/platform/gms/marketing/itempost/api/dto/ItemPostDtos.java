package com.g42.platform.gms.marketing.itempost.api.dto;

import com.g42.platform.gms.marketing.itempost.domain.ItemPostCtaPosition;
import com.g42.platform.gms.marketing.itempost.domain.ItemPostCtaType;
import com.g42.platform.gms.marketing.itempost.domain.ItemPostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Toàn bộ kiểu dữ liệu vào/ra của phân hệ bài viết phụ tùng, mirror
 * {@code PostDtos} nhưng thêm {@code catalogItemId} bắt buộc.
 */
public final class ItemPostDtos {

    private ItemPostDtos() {
    }

    /** Danh mục bài viết phụ tùng. */
    public record CategoryDto(
            Integer categoryId,
            String name,
            String slug,
            String description,
            Integer displayOrder,
            Boolean isActive,
            String seoTitle,
            String seoDescription,
            Long postCount
    ) {
    }

    public record CategorySaveRequest(
            @NotBlank(message = "Tên danh mục không được để trống")
            @Size(max = 120, message = "Tên danh mục tối đa 120 ký tự")
            String name,
            String slug,
            @Size(max = 500) String description,
            Integer displayOrder,
            Boolean isActive,
            @Size(max = 200) String seoTitle,
            @Size(max = 320) String seoDescription
    ) {
    }

    public record TagDto(Integer tagId, String name, String slug, Integer usageCount) {
    }

    /** Khối kêu gọi hành động chèn trong bài. */
    public record CtaDto(
            Long ctaId,
            ItemPostCtaType ctaType,
            String label,
            String subLabel,
            String targetUrl,
            Integer catalogItemId,
            ItemPostCtaPosition position,
            Integer displayOrder,
            Boolean isActive
    ) {
    }

    /** Bản rút gọn cho danh sách/thẻ bài. */
    public record SummaryDto(
            Long itemPostId,
            String slug,
            String title,
            String excerpt,
            String thumbnailUrl,
            Integer catalogItemId,
            String categoryName,
            String categorySlug,
            ItemPostStatus status,
            Boolean isFeatured,
            Integer pinnedOrder,
            LocalDateTime publishedAt,
            LocalDateTime scheduledAt,
            String authorName,
            Long viewCount,
            Integer readingMinutes,
            List<TagDto> tags
    ) {
    }

    /** Bản đầy đủ cho trang chi tiết công khai. */
    public record DetailDto(
            Long itemPostId,
            String slug,
            String title,
            String excerpt,
            String contentHtml,
            String thumbnailUrl,
            String coverUrl,
            Integer catalogItemId,
            String categoryName,
            String categorySlug,
            LocalDateTime publishedAt,
            LocalDateTime updatedAt,
            String authorName,
            String authorAvatar,
            String authorPosition,
            Long viewCount,
            Long shareCount,
            Integer readingMinutes,
            String seoTitle,
            String seoDescription,
            String seoKeywords,
            String ogImageUrl,
            String canonicalUrl,
            Boolean allowIndex,
            List<TagDto> tags,
            List<CtaDto> ctas,
            /** Có giá trị khi truy cập bằng slug cũ — FE phải chuyển hướng sang slug này. */
            String redirectSlug
    ) {
    }

    /** Bản quản trị: thêm thông tin duyệt bài và lịch đăng. */
    public record AdminDetailDto(
            Long itemPostId,
            String slug,
            String title,
            String excerpt,
            String contentHtml,
            String thumbnailUrl,
            String coverUrl,
            Integer catalogItemId,
            Integer categoryId,
            String categoryName,
            ItemPostStatus status,
            Boolean isFeatured,
            Integer pinnedOrder,
            LocalDateTime publishedAt,
            LocalDateTime scheduledAt,
            Integer authorStaffId,
            String authorName,
            Integer reviewerStaffId,
            String reviewerName,
            String reviewNote,
            Long viewCount,
            Long shareCount,
            Integer readingMinutes,
            String seoTitle,
            String seoDescription,
            String seoKeywords,
            String ogImageUrl,
            String canonicalUrl,
            Boolean allowIndex,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<TagDto> tags,
            List<CtaDto> ctas
    ) {
    }

    /** Dữ liệu tạo/sửa bài. */
    public record SaveRequest(
            @NotBlank(message = "Tiêu đề không được để trống")
            @Size(max = 500, message = "Tiêu đề tối đa 500 ký tự")
            String title,
            /** Bỏ trống thì hệ thống tự sinh từ tiêu đề. */
            @Size(max = 200) String slug,
            @NotNull(message = "Bài viết phải gắn với một phụ tùng")
            Integer catalogItemId,
            @Size(max = 500) String excerpt,
            String contentHtml,
            @Size(max = 500) String thumbnailUrl,
            @Size(max = 500) String coverUrl,
            Integer categoryId,
            Boolean isFeatured,
            Integer pinnedOrder,
            LocalDateTime scheduledAt,
            @Size(max = 200) String seoTitle,
            @Size(max = 320) String seoDescription,
            @Size(max = 500) String seoKeywords,
            @Size(max = 500) String ogImageUrl,
            @Size(max = 500) String canonicalUrl,
            Boolean allowIndex,
            /** Tên tag dạng chữ thường/hoa tuỳ ý; hệ thống tự tạo tag chưa tồn tại. */
            List<String> tags,
            List<CtaDto> ctas
    ) {
    }

    /** Đổi trạng thái bài. */
    public record StatusChangeRequest(
            ItemPostStatus status,
            /** Lý do trả bài về nháp, hiện cho tác giả. */
            @Size(max = 500) String reviewNote,
            /** Thời điểm hẹn đăng, chỉ dùng khi chuyển sang SCHEDULED. */
            LocalDateTime scheduledAt
    ) {
    }

    /** Thân yêu cầu ghi nhận lượt xem. */
    public record ViewRequest(
            String sessionKey,
            String referrer,
            String utmSource,
            String utmMedium,
            String utmCampaign,
            String utmContent,
            String device
    ) {
    }

    public record ViewResultDto(boolean counted, Long viewCount) {
    }

    /** Kết quả chạy backfill item_post cho toàn bộ catalog item chưa có bài viết. */
    public record BackfillResultDto(int created, int skipped) {
    }

    /** Thống kê cho màn hình quản trị. */
    public record StatsDto(
            long draft,
            long pending,
            long scheduled,
            long published,
            long archived,
            long totalViews
    ) {
    }
}
