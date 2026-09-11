package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.auth.entity.CustomerStatus;
import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.dto.CheckPhoneResponse.Status;
import com.g42.platform.gms.auth.entity.CustomerAuth;
import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.auth.repository.CustomerAuthRepository;
import com.g42.platform.gms.auth.repository.CustomerProfileRepository;
import com.g42.platform.gms.common.service.OtpService;
import com.g42.platform.gms.common.util.ContactMasking;
import com.g42.platform.gms.notification.domain.NotificationChannel;
import com.g42.platform.gms.notification.domain.NotificationRecipient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Đăng nhập khách hàng bằng SỐ ĐIỆN THOẠI HOẶC EMAIL.
 *
 * Định danh người dùng nhập được đưa qua {@link #resolveProfile(String)}: chứa "@" thì tra theo
 * email, còn lại tra theo số điện thoại — cùng cách làm với đăng nhập nhân viên
 * (StaffAuthDetailsService), nhưng dùng biến cục bộ để không dính lỗi tranh chấp giữa các
 * phiên đăng nhập đồng thời.
 */
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

    /* ================= 1. CHECK STATUS ================= */
    @Transactional(readOnly = true)
    public CheckPhoneResponse checkPhoneStatus(String identifier) {
        var profileOpt = findProfile(identifier);
        if (profileOpt.isEmpty()) {
            return new CheckPhoneResponse(Status.NOT_REGISTERED, false);
        }
        CustomerProfile profile = profileOpt.get();
        var authOpt = authRepo.findByCustomerId(profile.getCustomerId());

        Status status;
        boolean hasPin;
        if (authOpt.isPresent() && authOpt.get().getStatus() == CustomerStatus.LOCKED) {
            status = Status.LOCKED;
            hasPin = true;
        } else if (authOpt.isEmpty()
                || authOpt.get().getStatus() == CustomerStatus.INACTIVE
                || authOpt.get().getPinHash() == null) {
            status = Status.UNVERIFIED;
            hasPin = false;
        } else {
            status = Status.ACTIVE;
            hasPin = true;
        }

        CheckPhoneResponse response = new CheckPhoneResponse(status, hasPin);
        // Cho màn quên mật khẩu biết được phép chọn kênh nào, kèm giá trị đã che bớt để hiển thị
        response.setHasEmail(hasText(profile.getEmail()));
        response.setHasPhone(hasText(profile.getPhone()));
        response.setMaskedEmail(ContactMasking.maskEmail(profile.getEmail()));
        response.setMaskedPhone(ContactMasking.maskPhone(profile.getPhone()));
        return response;
    }

    /* ================= 2. REQUEST OTP ================= */

    /**
     * Gửi mã OTP cho khách.
     *
     * @param requestedChannel kênh khách tự chọn ở màn quên mật khẩu; để null thì hệ thống
     *                         tự quyết (nhập bằng email → gửi email, còn lại theo kênh trong hồ sơ)
     * @return kênh đã gửi thành công; ném AuthException(OTP_SEND_FAILED) nếu không gửi được qua kênh nào
     */
    @Transactional
    public NotificationChannel requestOtp(String identifier, NotificationChannel requestedChannel) {
        CustomerProfile profile = resolveProfile(identifier,
                "Số điện thoại hoặc email chưa đăng ký dịch vụ. Vui lòng liên hệ quầy lễ tân!");

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
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED.name(),
                    "Tài khoản đã bị khóa. Vui lòng liên hệ Admin.");
        }

        NotificationRecipient recipient = otpRecipient(profile, identifier, requestedChannel);
        return otpService.generateAndSendOtp(otpKeyOf(profile), recipient);
    }

    /* ================= 3. VERIFY OTP ================= */
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest req) {
        CustomerProfile profile = resolveProfile(req.getPhone(), "Không tìm thấy thông tin khách hàng");

        try {
            otpService.validateOtp(otpKeyOf(profile), req.getOtp());
        } catch (RuntimeException e) {
            throw new AuthException(AuthErrorCode.INVALID_OTP.name(), "Mã OTP không chính xác hoặc đã hết hạn");
        }

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

    /* ================= 4. SETUP PIN ================= */
    @Transactional
    public void setupPin(SetupPinRequest req) {
        if (!req.getPin().equals(req.getConfirmPin())) {
            throw new AuthException(AuthErrorCode.PIN_MISMATCH.name(), "PIN xác nhận không khớp");
        }
        CustomerProfile profile = resolveProfile(req.getPhone(), "User not found");
        CustomerAuth auth = authRepo.findByCustomerId(profile.getCustomerId())
                .orElseThrow(() -> new AuthException(AuthErrorCode.SYSTEM_ERROR.name(), "Auth record not found"));

        auth.setPinHash(passwordEncoder.encode(req.getPin()));
        auth.setStatus(CustomerStatus.ACTIVE);
        auth.setFailedAttemptCount(0);
        authRepo.save(auth);
    }

    /* ================= 5. LOGIN ================= */
    @Transactional(noRollbackFor = AuthException.class)
    public AuthResponse login(LoginRequest req) {
        CustomerProfile profile = resolveProfile(req.getPhone(), "Sai thông tin đăng nhập");

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
        // Subject luôn là SĐT trong hồ sơ, không phải chuỗi khách vừa nhập — nếu khách đăng nhập
        // bằng email thì phần còn lại của hệ thống (CustomerPrincipal#getPhone) vẫn nhận đúng SĐT.
        String token = jwtUtilCustomer.generateToken(profile.getPhone(), claims);

        return new AuthResponse("LOGIN_SUCCESS", "CUSTOMER", token);
    }

    /* ================= Định danh: SĐT hoặc email ================= */

    /**
     * Tra hồ sơ khách theo định danh: có "@" thì tìm theo email, còn lại tìm theo số điện thoại.
     * Cùng quy tắc với đăng nhập nhân viên, nhưng dùng biến cục bộ nên an toàn khi nhiều
     * người đăng nhập cùng lúc.
     */
    private Optional<CustomerProfile> findProfile(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String value = identifier.trim();
        if (isEmail(value)) {
            return profileRepo.findByEmailIgnoreCase(value);
        }
        return profileRepo.findByPhone(value);
    }

    private CustomerProfile resolveProfile(String identifier, String notFoundMessage) {
        return findProfile(identifier)
                .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), notFoundMessage));
    }

    private boolean isEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    /**
     * Khoá lưu OTP gắn với khách hàng chứ không gắn với chuỗi vừa nhập — nhờ vậy khách có thể
     * yêu cầu OTP bằng email rồi xác thực bằng số điện thoại (hoặc ngược lại) vẫn khớp.
     */
    private String otpKeyOf(CustomerProfile profile) {
        return "customer:" + profile.getCustomerId();
    }

    /**
     * Kênh gửi OTP, theo thứ tự ưu tiên:
     * 1. Kênh khách tự chọn ở màn quên mật khẩu (nếu tài khoản thực sự dùng được kênh đó)
     * 2. Khách nhập bằng email thì gửi thẳng vào email đó cho đúng kỳ vọng
     * 3. Kênh khách đã chọn trong hồ sơ
     *
     * Kênh nào lỗi thì dispatcher vẫn tự chuyển sang kênh còn lại để mã đến được tay khách.
     */
    private NotificationRecipient otpRecipient(CustomerProfile profile, String identifier,
                                               NotificationChannel requestedChannel) {
        NotificationChannel channel;
        if (requestedChannel != null) {
            requireChannelUsable(profile, requestedChannel);
            channel = requestedChannel;
        } else if (isEmail(identifier)) {
            channel = NotificationChannel.EMAIL;
        } else {
            channel = profile.getNotificationChannel();
        }
        return NotificationRecipient.of(profile.getPhone(), profile.getEmail(), channel);
    }

    /** Chặn trường hợp khách chọn kênh mà tài khoản chưa có thông tin liên hệ tương ứng. */
    private void requireChannelUsable(CustomerProfile profile, NotificationChannel channel) {
        if (channel == NotificationChannel.EMAIL && !hasText(profile.getEmail())) {
            throw new AuthException(AuthErrorCode.BAD_REQUEST.name(),
                    "Tài khoản chưa có email. Vui lòng chọn nhận mã qua Zalo hoặc liên hệ lễ tân để bổ sung email.");
        }
        if (channel == NotificationChannel.ZALO && !hasText(profile.getPhone())) {
            throw new AuthException(AuthErrorCode.BAD_REQUEST.name(),
                    "Tài khoản chưa có số điện thoại. Vui lòng chọn nhận mã qua Email.");
        }
    }

    private boolean hasText(String value) {
        return ContactMasking.hasText(value);
    }
}
