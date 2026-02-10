package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.auth.entity.CustomerStatus; // Import Enum Status của Customer
import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.dto.CheckPhoneResponse.Status; // Import Enum Status của Response
import com.g42.platform.gms.auth.entity.CustomerAuth;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.auth.repository.CustomerAuthRepository;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.common.service.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAuthService {

    private final CustomerProfileRepository profileRepo;
    private final CustomerAuthRepository authRepo;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtUtilCustomer jwtUtilCustomer;

    private static final int MAX_PIN_ATTEMPTS = 5;

    /* ================= 1. CHECK STATUS (CHO FRONTEND ĐIỀU HƯỚNG) ================= */
    @Transactional(readOnly = true)
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

        // Case 3: Chưa kích hoạt (Có hồ sơ nhưng chưa có Auth record HOẶC chưa có PIN HOẶC status là INACTIVE)
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

        // 4. Sinh OTP và gửi qua OtpService
        otpService.generateAndSendOtp(phone);
    }

    /* ================= 3. VERIFY OTP ================= */
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        // 1. Gọi Common Service để check OTP
        try {
            otpService.validateOtp(req.getPhone(), req.getOtp());
        } catch (RuntimeException e) {
            // Map lỗi Runtime sang AuthException để thống nhất response trả về
            throw new AuthException(AuthErrorCode.INVALID_OTP.name(), "Mã OTP không chính xác hoặc đã hết hạn");
        }

        // 2. Lấy thông tin user để reset số lần sai (nếu có)
        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Không tìm thấy thông tin khách hàng"));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR.name(), "Lỗi dữ liệu: Chưa khởi tạo bảo mật"));

        // 3. Reset số lần đăng nhập sai (nếu trước đó bị đếm nhưng chưa bị khóa)
        if (auth.getStatus() != CustomerStatus.LOCKED) {
            auth.setFailedAttemptCount(0);
            authRepo.save(auth);
        } else {
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED.name(), "Tài khoản đang bị khóa, không thể xác thực OTP.");
        }

        // Trả về thành công, Frontend sẽ chuyển sang màn hình "Tạo PIN"
        return new AuthResponse("OTP_VERIFIED", "CUSTOMER", null);
    }

    /* ================= 4. SETUP PIN ================= */
    @Transactional
    public void setupPin(SetupPinRequest req) {
        // 1. Validate khớp PIN
        if (!req.getPin().equals(req.getConfirmPin())) {
            throw new AuthException(AuthErrorCode.PIN_MISMATCH.name(), "PIN xác nhận không khớp");
        }

        // 2. Lấy Profile & Auth
        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "User not found"));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR.name(), "Auth record not found"));

        // 3. Cập nhật PIN và kích hoạt tài khoản
        auth.setPinHash(passwordEncoder.encode(req.getPin()));
        auth.setStatus(CustomerStatus.ACTIVE); // Chuyển trạng thái sang ACTIVE
        auth.setFailedAttemptCount(0);         // Reset đếm lỗi
        authRepo.save(auth);
    }

    /* ================= 5. LOGIN ================= */
    @Transactional(noRollbackFor = AuthException.class) // Không rollback transaction nếu login sai pass để còn lưu số lần sai
    public AuthResponse login(LoginRequest req) {
        // 1. Tìm Profile
        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Sai thông tin đăng nhập"));

        // 2. Tìm Auth
        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Sai thông tin đăng nhập"));

        // 3. Check Lock
        if (auth.getStatus() == CustomerStatus.LOCKED) {
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED.name(), "Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }

        // 4. Check PIN setup
        if (auth.getPinHash() == null) {
            throw new AuthException(AuthErrorCode.PIN_NOT_SET.name(), "Tài khoản chưa thiết lập mã PIN.");
        }

        // 5. Check Password (PIN)
        if (!passwordEncoder.matches(req.getPin(), auth.getPinHash())) {
            // Tăng số lần sai
            int newFailCount = auth.getFailedAttemptCount() + 1;
            auth.setFailedAttemptCount(newFailCount);

            String msg = "PIN không đúng.";

            if (newFailCount >= MAX_PIN_ATTEMPTS) {
                auth.setStatus(CustomerStatus.LOCKED);
                msg = "Tài khoản đã bị khóa do nhập sai PIN quá 5 lần.";
            } else {
                msg += " Còn " + (MAX_PIN_ATTEMPTS - newFailCount) + " lần thử.";
            }

            authRepo.save(auth);
            throw new AuthException(AuthErrorCode.INVALID_PIN.name(), msg);
        }

        // 6. Login thành công
        auth.setFailedAttemptCount(0);
        auth.setLastLoginAt(LocalDateTime.now());
        authRepo.save(auth);

        // 7. Tạo JWT
        Map<String, Object> claims = Map.of(
                "role", "CUSTOMER",
                "customerId", profile.getCustomerId(),
                "name", profile.getFullName()
        );
        String token = jwtUtilCustomer.generateToken(req.getPhone(), claims);

        return new AuthResponse("LOGIN_SUCCESS", "CUSTOMER", token);
    }
}