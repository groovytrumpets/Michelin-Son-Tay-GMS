package com.g42.platform.gms.marketing.recruitment.domain;

/**
 * Kết quả gửi email báo có hồ sơ mới về hộp thư quản lý.
 *
 * <p>Lưu lại thay vì chỉ ghi log vì hồ sơ ứng tuyển là dữ liệu người thật gửi
 * đi một lần: nếu thư không tới nơi, người tuyển phải nhìn thấy điều đó ngay
 * trên màn hình danh sách để còn mở hồ sơ ra xem tay.
 */
public enum NotifyStatus {
    /** Chưa gửi xong (thư đang xếp hàng gửi nền). */
    PENDING,
    /** Đã gửi thành công tới ít nhất một người nhận. */
    SENT,
    /** Gửi hỏng — xem thêm cột notify_error. */
    FAILED,
    /** Bỏ qua có chủ đích: quản lý tắt thông báo hoặc chưa cấu hình người nhận. */
    SKIPPED
}
