package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.dto.CheckPhoneResponse.Status; // Import Enum lồng
import com.g42.platform.gms.auth.entity.*;
import com.g42.platform.gms.auth.repository.CustomerAuthRepository;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.auth.exception.AuthException;
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

    // Cache OTP trong RAM (Có thể đổi sang Redis nếu cần)
    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();

    @Data
    @AllArgsConstructor
    private static class OtpEntry {
        private String otpHash;
        private LocalDateTime expiryTime;
        private int attemptCount;
    }

    /* ================= 1. CHECK STATUS (CHO FRONTEND ĐIỀU HƯỚNG) ================= */
    public CheckPhoneResponse checkPhoneStatus(String phone) {
        var profileOpt = profileRepo.findByPhone(phone);

        // Case 1: Chưa có hồ sơ (Chưa từng đến Garage)
        if (profileOpt.isEmpty()) {
            return new CheckPhoneResponse(Status.NOT_REGISTERED, false);
        }

        var authOpt = authRepo.findByCustomerId(profileOpt.get().getCustomerId());

        // Case 2: Đã bị khóa
        if (authOpt.isPresent() && authOpt.get().getStatus() == CustomerStatus.LOCKED) {
            return new CheckPhoneResponse(Status.LOCKED, true);
        }

        // Case 3: Chưa kích hoạt (Có hồ sơ nhưng chưa có Auth HOẶC chưa có PIN HOẶC status là INACTIVE)
        if (authOpt.isEmpty()
                || authOpt.get().getStatus() == CustomerStatus.INACTIVE
                || authOpt.get().getPinHash() == null) {
            return new CheckPhoneResponse(Status.UNVERIFIED, false);
        }

        // Case 4: Hoạt động bình thường
        return new CheckPhoneResponse(Status.ACTIVE, true);
    }

    /* ================= 2. REQUEST OTP (KÍCH HOẠT / QUÊN PIN) ================= */
    @Transactional
    public void requestOtp(String phone) {
        // 1. Kiểm tra Profile (Phải có do Lễ tân nhập trước)
        CustomerProfile profile = profileRepo.findByPhone(phone)
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.USER_NOT_FOUND.name(),
                        "Số điện thoại chưa đăng ký dịch vụ. Vui lòng liên hệ quầy lễ tân!"
                ));

        // 2. Kiểm tra/Tạo Auth
        // Nếu chưa có Auth -> Tạo mới với trạng thái INACTIVE
        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseGet(() -> {
                    CustomerAuth newAuth = new CustomerAuth();
                    newAuth.setCustomerId(profile.getCustomerId());
                    newAuth.setStatus(CustomerStatus.INACTIVE); // Mặc định là chưa kích hoạt
                    newAuth.setFailedAttemptCount(0);
                    newAuth.setCreatedAt(LocalDateTime.now());
                    return authRepo.save(newAuth);
                });

        // 3. Nếu tài khoản bị KHÓA thì chặn
        if (auth.getStatus() == CustomerStatus.LOCKED) {
            throw new AuthException(
                    AuthErrorCode.ACCOUNT_LOCKED.name(),
                    "Tài khoản đã bị khóa. Vui lòng liên hệ Admin."
            );
        }

        // 4. Sinh OTP và gửi
        generateAndCacheOtp(phone, "ACTIVATION_OR_RESET");
    }

    /* ================= 3. VERIFY OTP ================= */
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        OtpEntry entry = otpCache.get(req.getPhone());

        // --- Kiểm tra OTP ---
        if (entry == null) {
            throw new AuthException(AuthErrorCode.OTP_NOT_FOUND.name(), "OTP không tồn tại hoặc đã hết hạn");
        }
        if (entry.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpCache.remove(req.getPhone());
            throw new AuthException(AuthErrorCode.OTP_EXPIRED.name(), "OTP đã hết hạn");
        }
        if (!passwordEncoder.matches(req.getOtp(), entry.getOtpHash())) {
            entry.setAttemptCount(entry.getAttemptCount() + 1);
            if (entry.getAttemptCount() >= MAX_OTP_ATTEMPTS) {
                otpCache.remove(req.getPhone());
                throw new AuthException(AuthErrorCode.OTP_TOO_MANY_ATTEMPTS.name(), "Sai OTP quá số lần cho phép");
            }
            throw new AuthException(AuthErrorCode.OTP_INVALID.name(),
                    "OTP không đúng. Còn " + (MAX_OTP_ATTEMPTS - entry.getAttemptCount()) + " lần");
        }

        // --- OTP ĐÚNG -> XỬ LÝ DB ---
        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "User not found"));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR.name(), "Auth record missing"));

        // Reset bộ đếm lỗi
        auth.setFailedAttemptCount(0);
        authRepo.save(auth);
        otpCache.remove(req.getPhone());

        return new AuthResponse("OTP_VERIFIED", "CUSTOMER", null);
    }

    /* ================= 4. SETUP PIN ================= */
    @Transactional
    public void setupPin(SetupPinRequest req) {
        if (!req.getPin().equals(req.getConfirmPin())) {
            throw new AuthException(AuthErrorCode.PIN_MISMATCH.name(), "PIN xác nhận không khớp");
        }

        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "User not found"));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR.name(), "Auth not found"));

        auth.setPinHash(passwordEncoder.encode(req.getPin()));
        auth.setStatus(CustomerStatus.ACTIVE); // Đảm bảo trạng thái Active
        authRepo.save(auth);
    }

    /* ================= 5. LOGIN ================= */
    @Transactional(noRollbackFor = AuthException.class)
    public AuthResponse login(LoginRequest req) {
        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Sai thông tin đăng nhập"));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Sai thông tin đăng nhập"));

        if (auth.getStatus() == CustomerStatus.LOCKED) {
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED.name(), "Tài khoản đã bị khóa");
        }

        if (auth.getPinHash() == null) {
            throw new AuthException(AuthErrorCode.PIN_NOT_SET.name(), "Tài khoản chưa kích hoạt mã PIN");
        }

        if (!passwordEncoder.matches(req.getPin(), auth.getPinHash())) {
            auth.setFailedAttemptCount(auth.getFailedAttemptCount() + 1);
            if (auth.getFailedAttemptCount() >= MAX_PIN_ATTEMPTS) {
                auth.setStatus(CustomerStatus.LOCKED);
            }
            authRepo.save(auth);

            throw new AuthException(AuthErrorCode.INVALID_PIN.name(),
                    "PIN không đúng. Còn " + (MAX_PIN_ATTEMPTS - auth.getFailedAttemptCount()) + " lần");
        }

        // Login thành công
        auth.setFailedAttemptCount(0);
        auth.setLastLoginAt(LocalDateTime.now());
        authRepo.save(auth);

        String token = jwtUtil.generateToken(req.getPhone(), Map.of("role", "CUSTOMER"));

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