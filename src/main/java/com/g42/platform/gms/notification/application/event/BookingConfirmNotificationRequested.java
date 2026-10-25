package com.g42.platform.gms.notification.application.event;

import com.g42.platform.gms.notification.domain.NotificationRecipient;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Yêu cầu gửi thông báo xác nhận lịch hẹn cho khách.
 *
 * <p>Mang sẵn mọi dữ liệu cần gửi (đã tính trong giao dịch của người phát) chứ
 * không mang entity: người nghe chạy trên luồng nền sau khi giao dịch đã đóng,
 * nên không được phép đụng tới quan hệ lazy nào nữa.
 */
public record BookingConfirmNotificationRequested(
        NotificationRecipient recipient,
        String customerName,
        List<String> serviceNames,
        String code,
        LocalDateTime bookingTime,
        String garageLocation
) {
}
