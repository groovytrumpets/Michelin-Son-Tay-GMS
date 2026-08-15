package com.g42.platform.gms.notification.domain;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Kênh gửi thông báo cho khách hàng.
 *
 * Các hàm send* trả về true khi gửi THÀNH CÔNG, false khi thất bại (thiếu cấu hình,
 * lỗi mạng, hoặc nhà cung cấp trả về mã lỗi) — CustomerNotificationDispatcher dựa vào
 * giá trị này để tự chuyển sang kênh còn lại.
 */
@Repository
public interface NotificationSender {
    boolean sendBookingCf(String s, String nguyenVanA, String s1);
    boolean sendBookingConfirm(String phone, String customerName, List<String> productName, String orderCode, LocalDateTime bookingTime, String garageLocation);

    boolean sendOtpVerify(String number,String otp);

    boolean sendFeedback(String number, String name, String code);
    boolean sendEstimate(String number, String customerName,List<String> productName, String orderCode, LocalDateTime createAt, String garageLocation,String totalPrice);
}
