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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.g42.platform.gms.customer.infrastructure.entity.CustomerPhoneJpa;
import com.g42.platform.gms.customer.infrastructure.repository.CustomerPhoneJpaRepo;

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
    private final CustomerPhoneJpaRepo phoneRepo;

    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final String PRIMARY_PHONE_KEY = "primary";

    /** customerId → số khách đã chọn nhận OTP, chờ xác thực. Cùng vòng đời RAM với OtpService. */
    private final Map<Integer, String> otpPhoneByCustomer = new ConcurrentHashMap<>();
    /** customerId → số vừa xác thực OTP thành công, chờ bước đặt PIN để đôn lên số chính. */
    private final Map<Integer, String> verifiedPhoneByCustomer = new ConcurrentHashMap<>();

    /** Một số điện thoại của khách kèm key để chọn (số chính = "primary", số phụ = id customer_phone). */
    private record PhoneChoice(String key, String phone, boolean primary) {
    }

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

        // Khách nhiều số: trả danh sách đã che để màn đăng nhập/kích hoạt cho chọn 1 số
        String typed = identifier == null ? null : identifier.trim();
        for (PhoneChoice choice : phoneChoices(profile)) {
            response.getPhones().add(new CheckPhoneResponse.PhoneOption(
                    choice.key(), ContactMasking.maskPhone(choice.phone()), choice.primary(),
                    choice.phone().equals(typed)));
        }
        response.setHasPhone(!response.getPhones().isEmpty());
        return response;
    }

    /* ================= 2. REQUEST OTP ================= */

    /**
     * Gửi mã OTP cho khách.
     *
     * @param requestedChannel kênh khách tự chọn ở màn quên mật khẩu; để null thì hệ thống
     *                         tự quyết (nhập bằng email → gửi email, còn lại theo kênh trong hồ sơ)
     * @param phoneKey         khách nhiều số: số được chọn nhận mã (key trong CheckPhoneResponse.phones);
     *                         null thì dùng số khách vừa gõ, không thì số chính
     * @return kênh đã gửi thành công; ném AuthException(OTP_SEND_FAILED) nếu không gửi được qua kênh nào
     */
    @Transactional
    public NotificationChannel requestOtp(String identifier, NotificationChannel requestedChannel, String phoneKey) {
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

        String targetPhone = choosePhone(profile, phoneKey, identifier);
        NotificationRecipient recipient = otpRecipient(profile, targetPhone, identifier, requestedChannel);
        NotificationChannel sentVia = otpService.generateAndSendOtp(otpKeyOf(profile), recipient);
        if (targetPhone != null) {
            otpPhoneByCustomer.put(profile.getCustomerId(), targetPhone);
        } else {
            otpPhoneByCustomer.remove(profile.getCustomerId());
        }
        verifiedPhoneByCustomer.remove(profile.getCustomerId());
        return sentVia;
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

        String otpPhone = otpPhoneByCustomer.remove(profile.getCustomerId());
        if (otpPhone != null) verifiedPhoneByCustomer.put(profile.getCustomerId(), otpPhone);
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

        // Khách nhiều số đã chọn số nhận OTP và xác thực thành công số đó → số đó thành số chính
        // của tài khoản (số đăng nhập, số nhận Zalo). Chỉ đổi khi đúng số vừa xác thực.
        String verifiedPhone = verifiedPhoneByCustomer.remove(profile.getCustomerId());
        if (verifiedPhone != null && req.getPhoneKey() != null) {
            findChoice(profile, req.getPhoneKey())
                    .filter(choice -> choice.phone().equals(verifiedPhone) && !choice.primary())
                    .ifPresent(choice -> promoteToPrimary(profile, choice));
        }
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
        // Subject là SĐT của phiên đăng nhập, không phải chuỗi khách vừa nhập — nếu khách đăng nhập
        // bằng email thì phần còn lại của hệ thống (CustomerPrincipal#getPhone) vẫn nhận đúng SĐT.
        // Khách nhiều số thì là số khách chọn ở màn đăng nhập (VD để điền sẵn vào lịch hẹn).
        String sessionPhone = choosePhone(profile, req.getPhoneKey(), req.getPhone());
        String token = jwtUtilCustomer.generateToken(sessionPhone != null ? sessionPhone : profile.getPhone(), claims);

        return new AuthResponse("LOGIN_SUCCESS", "CUSTOMER", token);
    }

    /* ================= Định danh: SĐT hoặc email ================= */

    /**
     * Tra hồ sơ khách theo định danh: có "@" thì tìm theo email, còn lại tìm theo số điện thoại
     * (số chính hoặc số phụ). Cùng quy tắc với đăng nhập nhân viên, nhưng dùng biến cục bộ nên
     * an toàn khi nhiều người đăng nhập cùng lúc.
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

    /* ================= Khách nhiều số điện thoại ================= */

    /** Số chính trước, rồi các số phụ theo thứ tự thêm vào. */
    private List<PhoneChoice> phoneChoices(CustomerProfile profile) {
        List<PhoneChoice> choices = new ArrayList<>();
        if (hasText(profile.getPhone())) {
            choices.add(new PhoneChoice(PRIMARY_PHONE_KEY, profile.getPhone(), true));
        }
        for (CustomerPhoneJpa other : phoneRepo.findByCustomerIdOrderByCustomerPhoneIdAsc(profile.getCustomerId())) {
            choices.add(new PhoneChoice(String.valueOf(other.getCustomerPhoneId()), other.getPhone(), false));
        }
        return choices;
    }

    private Optional<PhoneChoice> findChoice(CustomerProfile profile, String phoneKey) {
        if (phoneKey == null || phoneKey.isBlank()) return Optional.empty();
        return phoneChoices(profile).stream().filter(c -> c.key().equals(phoneKey.trim())).findFirst();
    }

    /**
     * Số dùng cho OTP / phiên đăng nhập: số khách chọn → số khách vừa gõ (nếu là một số của
     * khách) → số chính. Key không thuộc khách này thì báo lỗi thay vì âm thầm dùng số khác.
     */
    private String choosePhone(CustomerProfile profile, String phoneKey, String identifier) {
        if (phoneKey != null && !phoneKey.isBlank()) {
            return findChoice(profile, phoneKey)
                    .map(PhoneChoice::phone)
                    .orElseThrow(() -> new AuthException(AuthErrorCode.BAD_REQUEST.name(),
                            "Số điện thoại đã chọn không thuộc tài khoản này. Vui lòng chọn lại."));
        }
        if (identifier != null && !isEmail(identifier)) {
            String typed = identifier.trim();
            Optional<String> matched = phoneChoices(profile).stream()
                    .map(PhoneChoice::phone).filter(typed::equals).findFirst();
            if (matched.isPresent()) return matched.get();
        }
        return hasText(profile.getPhone()) ? profile.getPhone() : null;
    }

    /** Đổi chỗ: số phụ vừa chọn lên làm số chính, số chính cũ xuống làm số phụ (không mất số nào). */
    private void promoteToPrimary(CustomerProfile profile, PhoneChoice choice) {
        CustomerPhoneJpa other = phoneRepo.findById(Integer.valueOf(choice.key())).orElse(null);
        if (other == null) return;
        String oldPrimary = profile.getPhone();
        if (hasText(oldPrimary)) {
            other.setPhone(oldPrimary);
            other.setNote("Số chính cũ");
            phoneRepo.save(other);
        } else {
            phoneRepo.delete(other);
        }
        phoneRepo.flush();
        profile.setPhone(choice.phone());
        profileRepo.save(profile);
        log.info("Customer {} chose phone {} as primary on activation", profile.getCustomerId(),
                ContactMasking.maskPhone(choice.phone()));
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
     * Mã qua Zalo đi tới {@code targetPhone} — số khách đã chọn khi có nhiều số.
     */
    private NotificationRecipient otpRecipient(CustomerProfile profile, String targetPhone, String identifier,
                                               NotificationChannel requestedChannel) {
        NotificationChannel channel;
        if (requestedChannel != null) {
            requireChannelUsable(profile, targetPhone, requestedChannel);
            channel = requestedChannel;
        } else if (isEmail(identifier)) {
            channel = NotificationChannel.EMAIL;
        } else {
            channel = profile.getNotificationChannel();
        }
        return NotificationRecipient.of(targetPhone, profile.getEmail(), channel);
    }

    /** Chặn trường hợp khách chọn kênh mà tài khoản chưa có thông tin liên hệ tương ứng. */
    private void requireChannelUsable(CustomerProfile profile, String targetPhone, NotificationChannel channel) {
        if (channel == NotificationChannel.EMAIL && !hasText(profile.getEmail())) {
            throw new AuthException(AuthErrorCode.BAD_REQUEST.name(),
                    "Tài khoản chưa có email. Vui lòng chọn nhận mã qua Zalo hoặc liên hệ lễ tân để bổ sung email.");
        }
        if (channel == NotificationChannel.ZALO && !hasText(targetPhone)) {
            throw new AuthException(AuthErrorCode.BAD_REQUEST.name(),
                    "Tài khoản chưa có số điện thoại. Vui lòng chọn nhận mã qua Email.");
        }
    }

    private boolean hasText(String value) {
        return ContactMasking.hasText(value);
    }
}
