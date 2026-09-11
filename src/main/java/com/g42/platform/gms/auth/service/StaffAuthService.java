package com.g42.platform.gms.auth.service;

import com.g42.platform.gms.auth.constant.AuthErrorCode;
import com.g42.platform.gms.auth.dto.*;
import com.g42.platform.gms.auth.entity.*;
import com.g42.platform.gms.auth.exception.AuthException;
import com.g42.platform.gms.auth.mapper.StaffAuthMapper;
import com.g42.platform.gms.auth.repository.StaffAuthRepo;
import com.g42.platform.gms.auth.repository.StaffProfileRepo;
import com.g42.platform.gms.auth.repository.StaffRoleRepository;
import com.g42.platform.gms.common.service.OtpService;
import com.g42.platform.gms.common.util.ContactMasking;
import com.g42.platform.gms.notification.domain.NotificationChannel;
import com.g42.platform.gms.notification.domain.NotificationRecipient;
import com.g42.platform.gms.systemlog.service.AuditRecord;
import com.g42.platform.gms.systemlog.service.AuditService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class StaffAuthService {
    @Autowired
    private StaffAuthRepo staffAuthRepo;
    @Autowired
    private StaffProfileRepo staffProfileRepo;
    private StaffRoleRepository staffRoleRepo;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JWTService jwtService;
    private final StaffAuthMapper staffAuthMapper; //Mapper giúp biến entity thành dto
    private static final int MAX_LOGIN_ATTEMPTS = 10;
    @Autowired
    private OtpService otpService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuditService auditService;

    public Iterable<StaffAuthDto> getAllStaffAuth() {
        return staffAuthRepo.findAll().stream().map(staffAuthMapper::toDto).toList();
    }

    public ResponseEntity<StaffAuthDto> getStaffAuthById(int id) {
        var staffAuth= staffAuthRepo.findById(id).orElse(null);
        if(staffAuth == null){
            return ResponseEntity.notFound().build(); //Cài 404 not found
        }
        return ResponseEntity.ok(staffAuthMapper.toDto(staffAuth)); //Mapper giúp biến entity thành dto
    }

    public StaffAuthDto AuthenticateStaff(String email, String password){
        var staffAuth = staffAuthRepo.searchByEmail(email);
        if(staffAuth == null){
            return staffAuthMapper.toDto(null);
        }
        StaffAuthDto staffAuthDto = staffAuthMapper.toDto((StaffAuth) staffAuth);
        if (staffAuthDto.getPasswordHash().equals(password)) {
            return staffAuthDto;
        }
        return null;
    }
    @Transactional(noRollbackFor = AuthException.class)
    public StaffAuthResponse verifyStaffAuth(LoginRequest loginRequest){
        if (loginRequest.getPhone()==null||loginRequest.getPin()==null){
            throw new AuthException(AuthErrorCode.BAD_REQUEST.name(), "Số điện thoại hoặc email là bắt buộc");
        }
        if (loginRequest.getPhone().contains("@")) {
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
            if (!loginRequest.getPhone().matches(emailRegex)) {
                throw new AuthException("Số điện thoại hoặc email không hợp lệ");
            }
        }else if (loginRequest.getPhone().matches("^[0-9+]+$")) {
            String phoneRegex = "^(0|\\+84)[0-9]{9}$";
            if (!loginRequest.getPhone().matches(phoneRegex)) {
                throw new AuthException("Số điện thoại hoặc email không hợp lệ");
            }
        }else {
            throw new AuthException("Số điện thoại hoặc email không hợp lệ");
        }

        try{
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhone(),
                        loginRequest.getPin()
                ));
            StaffPrincipal  staffPrincipal = (StaffPrincipal) authentication.getPrincipal();
            StaffAuth staffAuth = staffPrincipal.getStaffAuth();
        if (authentication.isAuthenticated()) {
            //todo: staffAccLocked
            if (!(staffPrincipal.isAccountNonLocked())){
                throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED.name(), "Tài khoản đã bị khóa hoặc chưa kích hoạt");
            }
            String token = jwtService.generateStaffJWToken(staffPrincipal.getAuthId());
            staffAuth.setFailedLoginCount(0);
            staffAuthRepo.save(staffAuth);
            System.err.println("STAFF LOGIN SUCCESS: ATTEMPT = "+staffAuth.getFailedLoginCount());
            //todo: staff have lot of roles
            StaffProfile staffProfile = staffProfileRepo.getStaffProfileByStaffauth_StaffAuthId(staffAuth.getStaffAuthId());
            List<String> roles = staffRoleRepo
                    .getStaffRoleByStaff_StaffId(staffProfile.getStaffId())
                    .stream()
                    .map(staffRole -> staffRole.getRole().getRoleCode())
                    .toList();
            try {
                AuditRecord.AuditRecordBuilder auditBuilder = AuditRecord.builder()
                        .action("LOGIN")
                        .module("AUTH")
                        .severity("INFO")
                        .actorStaffId(staffProfile.getStaffId())
                        .actorName(staffProfile.getFullName())
                        .actorRole(roles.isEmpty() ? null : roles.get(0))
                        .description("Đăng nhập thành công");
                auditService.fillRequestInfo(auditBuilder);
                auditService.record(auditBuilder.build());
            } catch (Exception ignored) {
            }
            return new StaffAuthResponse(staffProfile.getStaffId(),staffProfile.getFullName(),staffProfile.getAvatar(),"LOGIN_SUCCESS", roles, token);
        }
        }catch (BadCredentialsException e){
            System.err.println(e.getMessage());

        }
            throw new AuthException(AuthErrorCode.USER_NOT_FOUND.name(), "Sai thông tin đăng nhập, tài khoản có thể bị khóa sau 10 lần thử");
    }

    /**
     * Gửi mã OTP khôi phục mật khẩu cho nhân viên.
     *
     * @param requestedChannel kênh nhân viên tự chọn ("ZALO"/"EMAIL"); null thì hệ thống tự quyết
     *                         (nhập bằng email → gửi email, nhập bằng SĐT → gửi Zalo)
     * @return kênh đã gửi thành công; ném AuthException(OTP_SEND_FAILED) nếu không gửi được qua kênh nào
     */
    @Transactional
    public NotificationChannel requestOtpPhone(String identifier, NotificationChannel requestedChannel) {
        StaffAccount account = resolveStaff(identifier);
        if ("LOCKED".equals(account.auth().getStatus())) {
            throw new AuthException("Staff Account Locked", "STAFF_LOCKED");
        }
        return otpService.generateAndSendOtp(otpKeyOf(account), otpRecipient(account, identifier, requestedChannel));
    }

    /** Kênh liên hệ khả dụng của nhân viên, cho màn quên mật khẩu hiển thị lựa chọn. */
    @Transactional(readOnly = true)
    public ContactChannelsResponse getContactChannels(String identifier) {
        StaffAccount account = resolveStaff(identifier);
        return ContactChannelsResponse.of(account.profile().getPhone(), account.auth().getEmail());
    }

    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        // Phải tra lại nhân viên để lấy đúng khoá OTP: nhân viên có thể yêu cầu mã bằng email
        // rồi xác thực bằng SĐT (hoặc ngược lại), khoá phải gắn với tài khoản chứ không gắn
        // với chuỗi vừa nhập.
        StaffAccount account = resolveStaff(request.getPhone());
        try {
            otpService.validateOtp(otpKeyOf(account), request.getOtp());
        } catch (RuntimeException e) {
            throw new AuthException(AuthErrorCode.INVALID_OTP.name(), "Mã OTP không chính xác hoặc đã hết hạn");
        }
        return new AuthResponse("OTP_VERIFIED", "STAFF", null);
    }

    public void setupPassword(SetupPinRequest request) {
        if (!request.getPin().equals(request.getConfirmPin())) {
            throw new AuthException(AuthErrorCode.PIN_MISMATCH.name(), "PIN xác nhận không khớp");
        }
        StaffAuth staffAuth = resolveStaff(request.getPhone()).auth();
        staffAuth.setPasswordHash(passwordEncoder.encode(request.getPin()));
        staffAuth.setStatus("ACTIVE");
        staffAuth.setFailedLoginCount(0);
        staffAuthRepo.save(staffAuth);
    }

    /* ================= Định danh nhân viên: SĐT hoặc email ================= */

    /**
     * Email nằm ở bảng staff_auth, số điện thoại nằm ở staff_profile — nên tra theo định danh
     * nào cũng phải lấy đủ cả hai bản ghi.
     */
    private record StaffAccount(StaffAuth auth, StaffProfile profile) {
    }

    /**
     * Tra nhân viên theo định danh: có "@" thì tìm theo email, còn lại tìm theo số điện thoại.
     * Dùng biến cục bộ (không phải field của bean singleton như StaffAuthDetailsService)
     * để nhiều người khôi phục mật khẩu cùng lúc không lẫn tài khoản của nhau.
     */
    private StaffAccount resolveStaff(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new AuthException("Staff Not Found", "STAFF404");
        }
        String value = identifier.trim();
        StaffAuth staffAuth = null;
        StaffProfile staffProfile = null;

        if (value.contains("@")) {
            staffAuth = staffAuthRepo.findByEmail(value);
            if (staffAuth != null) {
                staffProfile = staffProfileRepo.getStaffProfileByStaffauth_StaffAuthId(staffAuth.getStaffAuthId());
            }
        } else {
            staffProfile = staffProfileRepo.findByPhone(value);
            if (staffProfile != null) {
                staffAuth = staffAuthRepo.findByStaffProfile(staffProfile);
            }
        }

        if (staffAuth == null || staffProfile == null) {
            throw new AuthException("Staff Not Found", "STAFF404");
        }
        return new StaffAccount(staffAuth, staffProfile);
    }

    /** Khoá lưu OTP gắn với nhân viên, không phụ thuộc chuỗi định danh vừa nhập. */
    private String otpKeyOf(StaffAccount account) {
        return "staff:" + account.profile().getStaffId();
    }

    /**
     * Kênh gửi OTP cho nhân viên. Nhân viên không có thiết lập kênh mặc định như khách hàng,
     * nên: ưu tiên kênh vừa chọn, không chọn thì suy từ kiểu định danh đã nhập.
     * Kênh chính lỗi thì dispatcher tự chuyển sang kênh còn lại.
     */
    private NotificationRecipient otpRecipient(StaffAccount account, String identifier,
                                               NotificationChannel requestedChannel) {
        String phone = account.profile().getPhone();
        String email = account.auth().getEmail();

        NotificationChannel channel;
        if (requestedChannel != null) {
            requireChannelUsable(requestedChannel, phone, email);
            channel = requestedChannel;
        } else {
            channel = identifier.contains("@") ? NotificationChannel.EMAIL : NotificationChannel.ZALO;
        }
        return NotificationRecipient.of(phone, email, channel);
    }

    private void requireChannelUsable(NotificationChannel channel, String phone, String email) {
        if (channel == NotificationChannel.EMAIL && !ContactMasking.hasText(email)) {
            throw new AuthException(AuthErrorCode.BAD_REQUEST.name(),
                    "Tài khoản chưa có email. Vui lòng chọn nhận mã qua Zalo.");
        }
        if (channel == NotificationChannel.ZALO && !ContactMasking.hasText(phone)) {
            throw new AuthException(AuthErrorCode.BAD_REQUEST.name(),
                    "Tài khoản chưa có số điện thoại. Vui lòng chọn nhận mã qua Email.");
        }
    }
}
