package com.g42.platform.gms.customercare.domain;

/**
 * Trạng thái chăm sóc hiện tại của một khách — KHÔNG lưu, luôn suy ra từ cuộc gọi gần
 * nhất (xem CustomerCareService.deriveStatus).
 */
public enum CareStatus {
    NOT_CALLED("Chưa gọi"),
    NO_ANSWER("Chưa liên lạc được"),
    FOLLOW_UP("Hẹn gọi lại"),
    BOOKED("Đã hẹn lịch"),
    CONTACTED("Đã liên lạc"),
    DECLINED("Không có nhu cầu"),
    STOPPED("Ngừng liên hệ"),
    NO_PHONE("Không có SĐT");

    private final String label;

    CareStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
