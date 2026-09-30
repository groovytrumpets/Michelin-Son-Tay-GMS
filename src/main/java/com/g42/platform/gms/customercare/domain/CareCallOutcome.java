package com.g42.platform.gms.customercare.domain;

/**
 * Kết quả một cuộc gọi chăm sóc khách.
 *
 * Tách từ hai ô tick "Đã gọi" / "Gọi thành công" và ô "Note" của file Excel cũ: ô Note ở
 * đó vừa là kết quả vừa là lời hẹn ("Không bắt máy", "Chặn số", "Để khách suy nghĩ thêm",
 * "Chưa đến kỳ kiểm tra", "Đang ở xa, khi nào về được thì thay"...). Gom những câu hay gặp
 * thành mã để lọc và đếm được, phần còn lại vẫn ghi tự do ở ghi chú.
 *
 * defaultFollowUpDays là số ngày gợi ý để gọi lại — giao diện điền sẵn, người gọi sửa được.
 * stopsContact = bật cờ customer_profile.do_not_contact, khách rời khỏi danh sách cần gọi.
 */
public enum CareCallOutcome {

    // ---- Không liên lạc được ----
    NO_ANSWER("Không bắt máy", false, 1, false),
    BUSY("Khách bận, gọi lại sau", false, 1, false),
    UNREACHABLE("Thuê bao / tắt máy", false, 3, false),
    WRONG_NUMBER("Sai số / số không còn dùng", false, null, true),
    BLOCKED("Chặn số", false, null, true),

    // ---- Đã nói chuyện với khách ----
    BOOKED("Đã hẹn lịch đến xưởng", true, null, false),
    WILL_VISIT("Sẽ qua, chưa chốt ngày", true, 7, false),
    THINKING("Khách suy nghĩ thêm", true, 7, false),
    NOT_DUE("Chưa đến kỳ bảo dưỡng", true, 60, false),
    AWAY("Khách đang ở xa", true, 30, false),
    DECLINED("Không có nhu cầu / không qua", true, null, false),
    REQUEST_STOP("Khách không muốn được gọi", true, null, true),
    OTHER("Khác", true, null, false);

    private final String label;
    private final boolean reached;
    private final Integer defaultFollowUpDays;
    private final boolean stopsContact;

    CareCallOutcome(String label, boolean reached, Integer defaultFollowUpDays, boolean stopsContact) {
        this.label = label;
        this.reached = reached;
        this.defaultFollowUpDays = defaultFollowUpDays;
        this.stopsContact = stopsContact;
    }

    public String getLabel() {
        return label;
    }

    public boolean isReached() {
        return reached;
    }

    public Integer getDefaultFollowUpDays() {
        return defaultFollowUpDays;
    }

    public boolean isStopsContact() {
        return stopsContact;
    }

    /** Mã lạ (dữ liệu cũ, bản FE lệch) trả về null thay vì ném lỗi. */
    public static CareCallOutcome parse(String code) {
        if (code == null || code.isBlank()) return null;
        try {
            return valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
