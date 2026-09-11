package com.g42.platform.gms.auth.dto;

import com.g42.platform.gms.notification.domain.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả gửi OTP — cho biết mã thực sự đi qua kênh nào, vì hệ thống có thể tự chuyển
 * sang kênh còn lại khi kênh chính gặp sự cố. Nhờ vậy màn hình báo đúng chỗ để khách đi tìm mã.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpSentResponse {
    /** Không bao giờ null — gửi thất bại cả hai kênh thì OtpService ném lỗi thay vì trả response này. */
    private NotificationChannel sentVia;
}
