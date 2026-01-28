package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.entity.*;
import com.g42.platform.gms.auth.repository.CustomerAuthRepository;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.common.constant.AuthErrorCode;
import com.g42.platform.gms.common.exception.AuthException;
import com.g42.platform.gms.security.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private final CustomerProfileRepository profileRepo;
    private final CustomerAuthRepository authRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final SecureRandom secureRandom = new SecureRandom();
    private static final int MAX_OTP_ATTEMPTS = 3;
    private static final int MAX_PIN_ATTEMPTS = 5;

    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class OtpEntry {
        private String otpHash;
        private LocalDateTime expiryTime;
        private int attemptCount;
    }

    /* ================= REGISTER ================= */

    public void register(RegisterRequest req) {
        profileRepo.findByPhone(req.getPhone()).ifPresent(profile -> {
            authRepo.findByCustomerId(profile.getCustomerId()).ifPresent(auth -> {
                if (auth.getPinHash() != null) {
                    throw new AuthException(
                            AuthErrorCode.PHONE_ALREADY_REGISTERED.name(),
                            "Số điện thoại đã được đăng ký"
                    );
                }
            });
        });

        generateAndCacheOtp(req.getPhone(), "REGISTER");
    }

    /* ================= FORGOT PIN ================= */

    public void forgotPin(String phone) {
        profileRepo.findByPhone(phone)
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.USER_NOT_FOUND.name(),
                        "Số điện thoại chưa đăng ký"
                ));

        generateAndCacheOtp(phone, "FORGOT_PIN");
    }

    /* ================= VERIFY OTP ================= */

    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        OtpEntry entry = otpCache.get(req.getPhone());

        if (entry == null) {
            throw new AuthException(AuthErrorCode.OTP_NOT_FOUND.name(), "OTP không tồn tại");
        }

        if (entry.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpCache.remove(req.getPhone());
            throw new AuthException(AuthErrorCode.OTP_EXPIRED.name(), "OTP đã hết hạn");
        }

        if (!passwordEncoder.matches(req.getOtp(), entry.getOtpHash())) {
            entry.setAttemptCount(entry.getAttemptCount() + 1);

            if (entry.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
                otpCache.remove(req.getPhone());
                throw new AuthException(
                        AuthErrorCode.OTP_TOO_MANY_ATTEMPTS.name(),
                        "Bạn đã nhập sai OTP quá số lần cho phép"
                );
            }

            throw new AuthException(
                    AuthErrorCode.OTP_INVALID.name(),
                    "OTP không đúng. Còn "
                            + (MAX_OTP_ATTEMPTS - entry.getAttemptCount()) + " lần"
            );
        }

        CustomerProfile profile = profileRepo.findByPhone(req.getPhone()).orElse(null);
        CustomerAuth auth;

        if (profile == null) {
            profile = new CustomerProfile();
            profile.setPhone(req.getPhone());
            profile.setFullName("User_" + req.getPhone());
            profile.setCreatedAt(LocalDateTime.now());
            profile = profileRepo.save(profile);

            auth = new CustomerAuth();
            auth.setCustomerId(profile.getCustomerId());
            auth.setStatus(CustomerStatus.ACTIVE);
            auth.setFailedAttemptCount(0);
            auth.setCreatedAt(LocalDateTime.now());
            authRepo.save(auth);
        } else {
            auth = authRepo.findByCustomerId(profile.getCustomerId())
                    .orElseThrow(() -> new AuthException(
                            AuthErrorCode.SYSTEM_ERROR.name(),
                            "Dữ liệu không nhất quán"
                    ));

            auth.setStatus(CustomerStatus.ACTIVE);
            auth.setFailedAttemptCount(0);
            authRepo.save(auth);
        }

        otpCache.remove(req.getPhone());
        return new AuthResponse("OTP_VERIFIED", "CUSTOMER", null);
    }

    /* ================= SETUP PIN ================= */

    @Transactional
    public void setupPin(SetupPinRequest req) {
        if (!req.getPin().equals(req.getConfirmPin())) {
            throw new AuthException(
                    AuthErrorCode.PIN_MISMATCH.name(),
                    "PIN xác nhận không khớp"
            );
        }

        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.USER_NOT_FOUND.name(),
                        "Không tìm thấy người dùng"
                ));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.SYSTEM_ERROR.name(),
                        "Auth không tồn tại"
                ));

        auth.setPinHash(passwordEncoder.encode(req.getPin()));
        authRepo.save(auth);
    }

    /* ================= LOGIN ================= */

    @Transactional(noRollbackFor = AuthException.class)
    public AuthResponse login(LoginRequest req) {
        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.USER_NOT_FOUND.name(),
                        "Sai thông tin đăng nhập"
                ));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.USER_NOT_FOUND.name(),
                        "Sai thông tin đăng nhập"
                ));

        if (auth.getStatus() == CustomerStatus.LOCKED) {
            throw new AuthException(
                    AuthErrorCode.ACCOUNT_LOCKED.name(),
                    "Tài khoản đã bị khóa"
            );
        }

        if (auth.getPinHash() == null) {
            throw new AuthException(
                    AuthErrorCode.PIN_NOT_SET.name(),
                    "Bạn chưa thiết lập PIN"
            );
        }

        if (!passwordEncoder.matches(req.getPin(), auth.getPinHash())) {
            auth.setFailedAttemptCount(auth.getFailedAttemptCount() + 1);

            if (auth.getFailedAttemptCount() >= MAX_PIN_ATTEMPTS) {
                auth.setStatus(CustomerStatus.LOCKED);
            }

            authRepo.save(auth);

            throw new AuthException(
                    AuthErrorCode.INVALID_PIN.name(),
                    "PIN không đúng. Còn "
                            + (MAX_PIN_ATTEMPTS - auth.getFailedAttemptCount()) + " lần"
            );
        }

        auth.setFailedAttemptCount(0);
        auth.setLastLoginAt(LocalDateTime.now());
        authRepo.save(auth);

        String token = jwtUtil.generateToken(
                req.getPhone(),
                Map.of("role", "CUSTOMER")
        );

        return new AuthResponse("LOGIN_SUCCESS", "CUSTOMER", token);
    }

    /* ================= HELPER ================= */

    private void generateAndCacheOtp(String phone, String purpose) {
        String otp = String.valueOf(secureRandom.nextInt(900000) + 100000);

        otpCache.put(phone, new OtpEntry(
                passwordEncoder.encode(otp),
                LocalDateTime.now().plusMinutes(5),
                0
        ));

        System.out.println(">>> [" + purpose + "] OTP cho " + phone + ": " + otp);
    }
}
