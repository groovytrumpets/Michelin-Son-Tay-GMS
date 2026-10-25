package com.g42.platform.gms.notification.application.event;

import com.g42.platform.gms.notification.application.service.CustomerNotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Gửi thông báo xác nhận lịch hẹn SAU khi giao dịch đã commit (AFTER_COMMIT) và
 * trên luồng nền (@Async): nhân viên bấm "Duyệt" không phải chờ Zalo/SMTP, và
 * Zalo/SMTP lỗi cũng không còn rollback việc duyệt lịch.
 *
 * <p>fallbackExecution = true để người phát nằm ngoài giao dịch vẫn gửi được.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingConfirmNotificationListener {

    private final CustomerNotificationDispatcher notificationDispatcher;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onBookingConfirmNotificationRequested(BookingConfirmNotificationRequested event) {
        try {
            notificationDispatcher.sendBookingConfirm(
                    event.recipient(),
                    event.customerName(),
                    event.serviceNames(),
                    event.code(),
                    event.bookingTime(),
                    event.garageLocation()
            );
        } catch (Exception e) {
            log.error("Gửi thông báo xác nhận lịch hẹn {} thất bại", event.code(), e);
        }
    }
}
