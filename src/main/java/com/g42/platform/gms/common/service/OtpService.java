package com.g42.platform.gms.common.service;

import com.g42.platform.gms.auth.exception.AuthException; // Hoặc tạo Exception chung
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class OtpService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int MAX_ATTEMPTS = 3;

    @Data
    @AllArgsConstructor
    private static class OtpEntry {
        private String otpHash;
        private LocalDateTime expiryTime;
        private int attemptCount;
    }

    /**
     * Sinh OTP và gửi (Giả lập)
     */
    public void generateAndSendOtp(String phone) {
        String otp = String.valueOf(secureRandom.nextInt(900000) + 100000);

        // Lưu vào RAM (Hash lại để bảo mật)
        otpCache.put(phone, new OtpEntry(
                passwordEncoder.encode(otp),
                LocalDateTime.now().plusMinutes(5), // Hết hạn sau 5 phút
                0
        ));

        System.out.println("🔥🔥🔥 [COMMON OTP] Gửi đến " + phone + ": " + otp);
        // Sau này tích hợp SMS API tại đây
    }

    /**
     * Xác thực OTP
     * Trả về true nếu đúng, ném Exception nếu sai
     */
    public boolean validateOtp(String phone, String inputOtp) {
        OtpEntry entry = otpCache.get(phone);

        if (entry == null) {
            throw new RuntimeException("OTP không tồn tại hoặc đã hết hạn");
        }

        if (entry.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpCache.remove(phone);
            throw new RuntimeException("OTP đã hết hạn");
        }

        if (!passwordEncoder.matches(inputOtp, entry.getOtpHash())) {
            entry.setAttemptCount(entry.getAttemptCount() + 1);
            if (entry.getAttemptCount() >= MAX_ATTEMPTS) {
                otpCache.remove(phone);
                throw new RuntimeException("Sai quá số lần cho phép. Vui lòng lấy mã mới.");
            }
            throw new RuntimeException("OTP không đúng.");
        }

        // OTP đúng -> Xóa khỏi cache để không dùng lại
        otpCache.remove(phone);
        return true;
    }
}