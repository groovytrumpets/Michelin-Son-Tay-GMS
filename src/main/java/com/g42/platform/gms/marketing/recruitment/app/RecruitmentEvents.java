package com.g42.platform.gms.marketing.recruitment.app;

/** Sự kiện nội bộ của phân hệ tuyển dụng. */
public final class RecruitmentEvents {

    private RecruitmentEvents() {
    }

    /**
     * Một hồ sơ vừa được ghi vào DB.
     *
     * <p>Chỉ mang id chứ không mang cả entity: người nghe chạy trên luồng khác
     * và sau khi giao dịch đã đóng, nên phải tự nạp lại bản ghi trong phiên của
     * chính nó thay vì cầm một entity đã tách khỏi phiên.
     */
    public record ApplicationSubmitted(Long applicationId) {
    }
}
