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

    /**
     * Kiểm tra trạng thái số điện thoại để frontend điều hướng
     */
    @Transactional(readOnly = true)
    public CheckPhoneResponse checkPhoneStatus(String phone) {
        var profileOpt = profileRepo.findByPhone(phone);

        if (profileOpt.isEmpty()) {
            return new CheckPhoneResponse(Status.NOT_REGISTERED, false);
        }

        var authOpt = authRepo.findByCustomerId(profileOpt.get().getCustomerId());

        if (authOpt.isPresent() && authOpt.get().getStatus() == CustomerStatus.LOCKED) {
            return new CheckPhoneResponse(Status.LOCKED, true);
        }

        if (authOpt.isEmpty()
                || authOpt.get().getStatus() == CustomerStatus.INACTIVE
                || authOpt.get().getPinHash() == null) {
            return new CheckPhoneResponse(Status.UNVERIFIED, false);
        }

        return new CheckPhoneResponse(Status.ACTIVE, true);
    }

    /**
     * Gửi OTP để kích hoạt tài khoản hoặc quên PIN
     */
    @Transactional
    public void requestOtp(String phone) {
        CustomerProfile profile = profileRepo.findByPhone(phone)
                .orElseThrow(() -> new AuthException(
                        AuthErrorCode.USER_NOT_FOUND.name(),
                        "Số điện thoại chưa đăng ký dịch vụ. Vui lòng liên hệ quầy lễ tân!"
                ));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseGet(() -> {
                    CustomerAuth newAuth = new CustomerAuth();
                    newAuth.setCustomerId(profile.getCustomerId());
                    newAuth.setStatus(CustomerStatus.INACTIVE);
                    newAuth.setFailedAttemptCount(0);
                    newAuth.setCreatedAt(LocalDateTime.now());
                    return authRepo.save(newAuth);
                });

        if (auth.getStatus() == CustomerStatus.LOCKED) {
            throw new AuthException(
                    AuthErrorCode.ACCOUNT_LOCKED.name(),
                    "Tài khoản đã bị khóa. Vui lòng liên hệ Admin."
            );
        }

        otpService.generateAndSendOtp(phone);
    }

    /**
     * Xác thực OTP
     */
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        try {
            otpService.validateOtp(req.getPhone(), req.getOtp());
        } catch (RuntimeException e) {
            throw new AuthException(AuthErrorCode.INVALID_OTP.name(), "Mã OTP không chính xác hoặc đã hết hạn");
        }

        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Không tìm thấy thông tin khách hàng"));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR.name(), "Lỗi dữ liệu: Chưa khởi tạo bảo mật"));

        if (auth.getStatus() != CustomerStatus.LOCKED) {
            auth.setFailedAttemptCount(0);
            authRepo.save(auth);
        } else {
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED.name(), "Tài khoản đang bị khóa, không thể xác thực OTP.");
        }

        return new AuthResponse("OTP_VERIFIED", "CUSTOMER", null);
    }

    /**
     * Thiết lập mã PIN
     */
    @Transactional
    public void setupPin(SetupPinRequest req) {
        if (!req.getPin().equals(req.getConfirmPin())) {
            throw new AuthException(AuthErrorCode.PIN_MISMATCH.name(), "PIN xác nhận không khớp");
        }

        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "User not found"));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR.name(), "Auth record not found"));

        auth.setPinHash(passwordEncoder.encode(req.getPin()));
        auth.setStatus(CustomerStatus.ACTIVE);
        auth.setFailedAttemptCount(0);
        authRepo.save(auth);
    }

    /**
     * Đăng nhập bằng số điện thoại và PIN
     */
    @Transactional(noRollbackFor = AuthException.class)
    public AuthResponse login(LoginRequest req) {
        CustomerProfile profile = profileRepo.findByPhone(req.getPhone())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Sai thông tin đăng nhập"));

        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Sai thông tin đăng nhập"));

        if (auth.getStatus() == CustomerStatus.LOCKED) {
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED.name(), "Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }

        if (auth.getPinHash() == null) {
            throw new AuthException(AuthErrorCode.PIN_NOT_SET.name(), "Tài khoản chưa thiết lập mã PIN.");
        }

        if (!passwordEncoder.matches(req.getPin(), auth.getPinHash())) {
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

        auth.setFailedAttemptCount(0);
        auth.setLastLoginAt(LocalDateTime.now());
        authRepo.save(auth);

        Map<String, Object> claims = Map.of(
                "role", "CUSTOMER",
                "customerId", profile.getCustomerId(),
                "name", profile.getFullName()
        );
        String token = jwtUtilCustomer.generateToken(req.getPhone(), claims);

        return new AuthResponse("LOGIN_SUCCESS", "CUSTOMER", token);
    }
}