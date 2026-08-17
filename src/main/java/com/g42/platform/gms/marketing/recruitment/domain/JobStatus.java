package com.g42.platform.gms.marketing.recruitment.domain;

/**
 * Vòng đời của một tin tuyển dụng.
 *
 * <pre>
 * DRAFT ──gửi duyệt──> PENDING ──duyệt──> PUBLISHED ──đủ người/hết hạn──> CLOSED
 *   ▲                     │                                                  │
 *   └──── trả lại ────────┘                                                  ▼
 *                                                                       ARCHIVED
 * </pre>
 *
 * <p>Khác {@code PostStatus}: không có SCHEDULED (tin tuyển dụng đăng là chạy
 * ngay) nhưng có CLOSED — trạng thái "còn xem được, hết nhận hồ sơ". Đóng tin
 * thay vì gỡ hẳn để link đã dán lên Facebook/nhóm nghề vẫn mở ra được trang
 * đàng hoàng thay vì trang 404.
 */
public enum JobStatus {
    /** Đang soạn, chỉ người trong khu quản trị thấy. */
    DRAFT,
    /** Đã gửi và đang chờ quản lý duyệt. */
    PENDING,
    /** Đang hiển thị công khai và còn nhận hồ sơ. */
    PUBLISHED,
    /** Còn xem được nhưng form đăng ký đã khoá. */
    CLOSED,
    /** Gỡ khỏi danh sách; chỉ còn truy cập được bằng đường dẫn trực tiếp. */
    ARCHIVED;

    /** Tin còn hiện ngoài trang danh sách của khách. */
    public boolean isListedToPublic() {
        return this == PUBLISHED || this == CLOSED;
    }

    /** Chỉ tin đang mở mới cho gửi hồ sơ. */
    public boolean acceptsApplications() {
        return this == PUBLISHED;
    }
}
