package com.g42.platform.gms.common.service;

import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.notification.application.service.CustomerNotificationDispatcher;
import com.g42.platform.gms.notification.domain.NotificationChannel;
import com.g42.platform.gms.notification.domain.NotificationRecipient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@AllArgsConstructor
public class OtpService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final CustomerNotificationDispatcher notificationDispatcher;
    private final CustomerProfileRepository customerProfileRepository;

    private static final int MAX_ATTEMPTS = 3;

    @Data
    @AllArgsConstructor
    private static class OtpEntry {
        private String otpHash;
        private LocalDateTime expiryTime;
        private int attemptCount;
    }

    /**
     * Sinh OTP theo số điện thoại, tự tra hồ sơ khách để chọn kênh gửi.
     * OTP được lưu trong cache với khoá chính là số điện thoại truyền vào.
     */
    public NotificationChannel generateAndSendOtp(String phone) {
        // Khách đã có hồ sơ + chọn Email thì gửi OTP qua email, còn lại (kể cả người chưa có tài khoản) gửi qua Zalo
        NotificationRecipient recipient = customerProfileRepository.findByPhone(phone)
                .map(c -> NotificationRecipient.of(c.getPhone(), c.getEmail(), c.getNotificationChannel()))
                .orElse(NotificationRecipient.phoneOnly(phone));
        return generateAndSendOtp(phone, recipient);
    }

    /**
     * Sinh OTP với khoá cache do phía gọi quyết định.
     *
     * Dùng cho luồng đăng nhập khách hàng — khách có thể nhập SĐT hoặc email nên phía gọi
     * đã tra hồ sơ và truyền vào một khoá cố định theo khách (không phụ thuộc chuỗi đã nhập),
     * để yêu cầu OTP bằng email rồi xác thực bằng SĐT (hoặc ngược lại) vẫn khớp.
     *
     * @param otpKey    khoá lưu OTP, phải dùng đúng khoá này khi gọi validateOtp
     * @param recipient người nhận + kênh gửi ưu tiên
     */
    public NotificationChannel generateAndSendOtp(String otpKey, NotificationRecipient recipient) {
        String otp = String.valueOf(secureRandom.nextInt(900000) + 100000);

        // Lưu vào RAM (Hash lại để bảo mật)
        otpCache.put(otpKey, new OtpEntry(
                passwordEncoder.encode(otp),
                LocalDateTime.now().plusMinutes(5), // Hết hạn sau 5 phút
                0
        ));

        NotificationChannel sentVia = notificationDispatcher.sendOtpVerify(recipient, otp);
        if (sentVia == null) {
            // Cả Zalo lẫn Email đều không gửi được — khách sẽ không bao giờ nhận được mã này,
            // nên báo lỗi ngay thay vì để khách chờ ở màn nhập OTP. Vẫn in ra log ở mức WARN để
            // dev/ops còn tra cứu được (không phải cơ chế cho khách đăng nhập bằng log nữa).
            log.warn("Không gửi được OTP qua kênh nào cho [{}], mã tạm thời: {}", otpKey, otp);
            otpCache.remove(otpKey);
            throw new AuthException(AuthErrorCode.OTP_SEND_FAILED.name(),
                    "Không thể gửi mã OTP qua Zalo hoặc Email lúc này. Vui lòng thử lại sau.");
        }
        // Sau này tích hợp SMS API tại đây
        return sentVia;
    }

    /**
     * Xác thực OTP
     * Trả về true nếu đúng, ném Exception nếu sai
     *
     * @param otpKey khoá đã dùng lúc sinh OTP (số điện thoại, hoặc khoá do luồng gọi quy định)
     */
    public boolean validateOtp(String otpKey, String inputOtp) {
        OtpEntry entry = otpCache.get(otpKey);

        if (entry == null) {
            throw new RuntimeException("OTP không tồn tại hoặc đã hết hạn");
        }

        if (entry.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpCache.remove(otpKey);
            throw new RuntimeException("OTP đã hết hạn");
        }

        if (!passwordEncoder.matches(inputOtp, entry.getOtpHash())) {
            entry.setAttemptCount(entry.getAttemptCount() + 1);
            if (entry.getAttemptCount() >= MAX_ATTEMPTS) {
                otpCache.remove(otpKey);
                throw new RuntimeException("Sai quá số lần cho phép. Vui lòng lấy mã mới.");
            }
            throw new RuntimeException("OTP không đúng.");
        }

        // OTP đúng -> Xóa khỏi cache để không dùng lại
        otpCache.remove(otpKey);
        return true;
    }
}