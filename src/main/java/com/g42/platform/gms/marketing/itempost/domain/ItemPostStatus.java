package com.g42.platform.gms.marketing.itempost.domain;

/**
 * Vòng đời của một bài viết phụ tùng.
 *
 * <pre>
 * DRAFT ──gửi duyệt──> PENDING ──duyệt──> PUBLISHED ──> ARCHIVED
 *   ▲                     │
 *   └──── trả lại ────────┘
 * DRAFT ──hẹn giờ──> SCHEDULED ──(job nền)──> PUBLISHED
 * </pre>
 */
public enum ItemPostStatus {
    /** Đang soạn, chỉ tác giả thấy. */
    DRAFT,
    /** Đã gửi và đang chờ quản lý duyệt. */
    PENDING,
    /** Đã duyệt, chờ tới giờ hẹn để tự đăng. */
    SCHEDULED,
    /** Đang hiển thị công khai. */
    PUBLISHED,
    /** Gỡ khỏi danh sách nhưng vẫn còn link để không chết đường dẫn đã chia sẻ. */
    ARCHIVED
}
