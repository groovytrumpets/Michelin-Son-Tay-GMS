package com.g42.platform.gms.notification.application.service;

import com.g42.platform.gms.notification.domain.NotificationChannel;
import com.g42.platform.gms.notification.domain.NotificationRecipient;
import com.g42.platform.gms.notification.infrastructure.EmailNotificationSender;
import com.g42.platform.gms.notification.infrastructure.ZaloNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Điểm gửi thông báo duy nhất cho nghiệp vụ khách hàng.
 *
 * Kênh chính lấy theo lựa chọn của khách (NotificationRecipient#shouldUseEmail()).
 * Nếu kênh chính gửi thất bại — Zalo hết token / trả mã lỗi, hoặc SMTP chưa cấu hình /
 * gửi lỗi — hệ thống TỰ CHUYỂN sang kênh còn lại nếu kênh đó dùng được. Nhờ vậy
 * thông báo quan trọng (OTP, xác nhận lịch) không mất khi một nhà cung cấp gặp sự cố.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerNotificationDispatcher {

    private final ZaloNotificationSender zaloNotificationSender;
    private final EmailNotificationSender emailNotificationSender;

    public boolean sendBookingConfirm(NotificationRecipient recipient, String customerName, List<String> productName,
                                    String orderCode, LocalDateTime bookingTime, String garageLocation) {
        return null != dispatch(recipient, "xác nhận lịch hẹn " + orderCode,
                () -> zaloNotificationSender.sendBookingConfirm(recipient.phone(), customerName, productName, orderCode, bookingTime, garageLocation),
                () -> emailNotificationSender.sendBookingConfirm(recipient.email(), customerName, productName, orderCode, bookingTime, garageLocation));
    }

    /** @return kênh đã gửi thành công (có thể khác kênh ưu tiên nếu phải chuyển kênh), null nếu không gửi được */
    public NotificationChannel sendOtpVerify(NotificationRecipient recipient, String otp) {
        return dispatch(recipient, "mã OTP",
                () -> zaloNotificationSender.sendOtpVerify(recipient.phone(), otp),
                () -> emailNotificationSender.sendOtpVerify(recipient.email(), otp));
    }

    public boolean sendFeedback(NotificationRecipient recipient, String name, String code) {
        return null != dispatch(recipient, "khảo sát hài lòng " + code,
                () -> zaloNotificationSender.sendFeedback(recipient.phone(), name, code),
                () -> emailNotificationSender.sendFeedback(recipient.email(), name, code));
    }

    public boolean sendEstimate(NotificationRecipient recipient, String customerName, List<String> productName,
                              String orderCode, LocalDateTime createAt, String garageLocation, String totalPrice) {
        return null != dispatch(recipient, "báo giá " + orderCode,
                () -> zaloNotificationSender.sendEstimate(recipient.phone(), customerName, productName, orderCode, createAt, garageLocation, totalPrice),
                () -> emailNotificationSender.sendEstimate(recipient.email(), customerName, productName, orderCode, createAt, garageLocation, totalPrice));
    }

    /**
     * Gửi qua kênh chính, thất bại thì tự chuyển sang kênh còn lại.
     *
     * @return kênh đã gửi thành công, hoặc null nếu cả hai kênh đều không gửi được
     */
    private NotificationChannel dispatch(NotificationRecipient recipient, String context,
                                         BooleanSupplier viaZalo, BooleanSupplier viaEmail) {
        boolean preferEmail = recipient.shouldUseEmail();
        NotificationChannel primary = preferEmail ? NotificationChannel.EMAIL : NotificationChannel.ZALO;
        NotificationChannel secondary = preferEmail ? NotificationChannel.ZALO : NotificationChannel.EMAIL;

        boolean sent = preferEmail ? viaEmail.getAsBoolean() : viaZalo.getAsBoolean();
        if (sent) {
            return primary;
        }

        boolean fallbackAvailable = preferEmail ? recipient.canUseZalo() : recipient.canUseEmail();
        if (!fallbackAvailable) {
            log.error("Không gửi được [{}] qua {} và không có kênh thay thế", context, primary);
            return null;
        }

        log.warn("Gửi [{}] qua {} thất bại, đang thử lại bằng {}", context, primary, secondary);
        boolean sentByFallback = preferEmail ? viaZalo.getAsBoolean() : viaEmail.getAsBoolean();

        if (sentByFallback) {
            log.info("Đã gửi [{}] bằng kênh thay thế {}", context, secondary);
            return secondary;
        }
        log.error("Không gửi được [{}] qua cả Zalo lẫn Email", context);
        return null;
    }
}
