package com.g42.platform.gms.report.application.service;

import com.g42.platform.gms.auth.entity.StaffPrincipal;
import com.g42.platform.gms.common.util.ContactMasking;
import com.g42.platform.gms.notification.infrastructure.EmailNotificationSender;
import com.g42.platform.gms.notification.infrastructure.EmailNotificationSender.MailDeliveryException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Xác minh bằng email trước khi xuất báo cáo tổng hợp (/report-export).
 *
 * <p>File xuất chứa toàn bộ danh bạ khách + giá vốn / lợi nhuận. Chỉ có mã quyền thì ai mượn
 * được máy đang đăng nhập (hoặc lộ mật khẩu) là mang được dữ liệu ra ngoài; bắt nhập mã gửi về
 * email của CHÍNH tài khoản đó thêm một lớp "người đang ngồi máy có giữ hộp thư của tài khoản".
 *
 * <p>Luồng: {@link #sendCode} gửi mã 6 số (5 phút, sai {@value #MAX_ATTEMPTS} lần là huỷ) →
 * {@link #verify} đổi mã lấy một phiên xuất ({@link #GRANT_TTL}) → mỗi lần tải file gọi
 * {@link #requireGrant}. Phiên gắn với tài khoản + email lúc xác minh, chỉ giữ trong RAM
 * (khởi động lại BE là phải xác minh lại — chấp nhận được, và không lưu mã xuống DB).
 *
 * <p>Gửi email hỏng thì báo lỗi luôn, KHÔNG in mã ra log như OtpService: log xem được ở
 * /backend-logs, in mã ra đó là tự mở cửa sau.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportVerificationService {

    public static final Duration CODE_TTL = Duration.ofMinutes(5);
    public static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    public static final Duration GRANT_TTL = Duration.ofMinutes(15);
    public static final int MAX_ATTEMPTS = 5;

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");

    private final EmailNotificationSender emailSender;
    private final SecureRandom random = new SecureRandom();
    /** staff_auth_id → mã đang chờ nhập. */
    private final Map<Integer, PendingCode> codes = new ConcurrentHashMap<>();
    /** staff_auth_id → phiên xuất đã xác minh. */
    private final Map<Integer, Grant> grants = new ConcurrentHashMap<>();

    private record PendingCode(byte[] salt, byte[] hash, String email, Instant expiresAt, Instant sentAt,
                               int attempts) {
        PendingCode withAttempt() {
            return new PendingCode(salt, hash, email, expiresAt, sentAt, attempts + 1);
        }
    }

    private record Grant(byte[] tokenHash, String email, Instant expiresAt) {}

    /** Trạng thái hiển thị cho màn hình. */
    public record Status(boolean hasEmail, String maskedEmail, boolean emailConfigured,
                         boolean codePending, Instant codeExpiresAt, Instant resendAvailableAt,
                         Instant verifiedUntil) {}

    public record Challenge(String maskedEmail, Instant expiresAt, Instant resendAvailableAt,
                            int maxAttempts) {}

    public record GrantInfo(String exportToken, Instant expiresAt, String maskedEmail) {}

    /** Kết quả nhập sai — để controller ghi nhật ký khi bị khoá. */
    public static class VerificationFailedException extends ResponseStatusException {
        private final boolean locked;

        VerificationFailedException(String message, boolean locked) {
            super(HttpStatus.BAD_REQUEST, message);
            this.locked = locked;
        }

        public boolean isLocked() {
            return locked;
        }
    }

    public Status status(StaffPrincipal principal) {
        purgeExpired();
        String email = emailOf(principal);
        Integer key = principal.getAuthId();
        PendingCode code = codes.get(key);
        Grant grant = grants.get(key);
        boolean grantValid = grant != null && email != null && grant.email.equalsIgnoreCase(email);
        return new Status(email != null, email != null ? ContactMasking.maskEmail(email) : null,
                emailSender.isConfigured(),
                code != null, code != null ? code.expiresAt : null,
                code != null ? code.sentAt.plus(RESEND_COOLDOWN) : null,
                grantValid ? grant.expiresAt : null);
    }

    public Challenge sendCode(StaffPrincipal principal, String requestIp) {
        purgeExpired();
        String email = requireEmail(principal);
        Integer key = principal.getAuthId();
        Instant now = Instant.now();

        PendingCode previous = codes.get(key);
        if (previous != null && previous.sentAt.plus(RESEND_COOLDOWN).isAfter(now)) {
            long wait = Duration.between(now, previous.sentAt.plus(RESEND_COOLDOWN)).toSeconds() + 1;
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Vừa gửi mã rồi, vui lòng đợi " + wait + " giây trước khi gửi lại.");
        }
        if (!emailSender.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Máy chủ chưa cấu hình gửi email (SMTP) nên chưa gửi được mã xác minh. "
                            + "Liên hệ quản trị viên để điền MAIL_SMTP_* rồi thử lại.");
        }

        String code = String.valueOf(100000 + random.nextInt(900000));
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        String masked = ContactMasking.maskEmail(email);
        try {
            emailSender.sendHtmlOrThrow(List.of(email), null,
                    "Mã xác minh xuất báo cáo tổng hợp: " + code,
                    buildEmail(principal, code, requestIp));
        } catch (MailDeliveryException e) {
            // Lý do chi tiết đã nằm trong log của EmailNotificationSender.
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Không gửi được email xác minh tới " + masked + ". Vui lòng thử lại sau hoặc báo quản trị viên kiểm tra cấu hình email.");
        }
        PendingCode pending = new PendingCode(salt, hash(salt, code), email.toLowerCase(),
                now.plus(CODE_TTL), now, 0);
        codes.put(key, pending);
        return new Challenge(masked, pending.expiresAt, pending.sentAt.plus(RESEND_COOLDOWN), MAX_ATTEMPTS);
    }

    public GrantInfo verify(StaffPrincipal principal, String rawCode) {
        purgeExpired();
        String email = requireEmail(principal);
        Integer key = principal.getAuthId();
        String code = rawCode == null ? "" : rawCode.replaceAll("\\s+", "");
        if (!code.matches("\\d{6}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác minh gồm 6 chữ số.");
        }

        PendingCode pending = codes.get(key);
        if (pending == null || !pending.email.equalsIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mã đã hết hạn hoặc chưa được gửi. Bấm \"Gửi mã\" để nhận mã mới.");
        }
        if (!MessageDigest.isEqual(pending.hash, hash(pending.salt, code))) {
            PendingCode next = pending.withAttempt();
            if (next.attempts >= MAX_ATTEMPTS) {
                codes.remove(key);
                throw new VerificationFailedException(
                        "Nhập sai quá " + MAX_ATTEMPTS + " lần, mã đã bị huỷ. Vui lòng gửi mã mới.", true);
            }
            codes.put(key, next);
            throw new VerificationFailedException(
                    "Mã không đúng. Còn " + (MAX_ATTEMPTS - next.attempts) + " lần thử.", false);
        }

        codes.remove(key);
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Grant grant = new Grant(hash(new byte[0], token), email.toLowerCase(), Instant.now().plus(GRANT_TTL));
        grants.put(key, grant);
        return new GrantInfo(token, grant.expiresAt, ContactMasking.maskEmail(email));
    }

    /** Ném 428 nếu tài khoản chưa xác minh, phiên hết hạn, hoặc token không phải của tài khoản này. */
    public void requireGrant(StaffPrincipal principal, String token) {
        purgeExpired();
        Grant grant = grants.get(principal.getAuthId());
        String email = emailOf(principal);
        if (token == null || token.isBlank() || grant == null || email == null
                || !grant.email.equalsIgnoreCase(email)
                || !MessageDigest.isEqual(grant.tokenHash, hash(new byte[0], token.trim()))) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "Cần xác minh email trước khi xuất báo cáo (phiên xác minh đã hết hạn hoặc không hợp lệ).");
        }
    }

    public void revoke(StaffPrincipal principal) {
        grants.remove(principal.getAuthId());
        codes.remove(principal.getAuthId());
    }

    // ------------------------------------------------------------------------------------------

    private String requireEmail(StaffPrincipal principal) {
        String email = emailOf(principal);
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Tài khoản của bạn chưa có email nên không xác minh được. "
                            + "Nhờ quản trị viên thêm email cho tài khoản ở Quản lý nhân viên.");
        }
        return email;
    }

    private static String emailOf(StaffPrincipal principal) {
        if (principal == null || principal.getStaffAuth() == null) return null;
        String email = principal.getStaffAuth().getEmail();
        return email != null && email.contains("@") ? email.trim() : null;
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        codes.values().removeIf(c -> c.expiresAt.isBefore(now));
        grants.values().removeIf(g -> g.expiresAt.isBefore(now));
    }

    private static byte[] hash(byte[] salt, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }

    private static String buildEmail(StaffPrincipal principal, String code, String ip) {
        String name = principal.getStaffAuth().getStaffProfile() != null
                ? principal.getStaffAuth().getStaffProfile().getFullName() : null;
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:540px;color:#172033\">"
                + "<h2 style=\"margin:0 0 12px;color:#123b7a\">Xác minh xuất báo cáo tổng hợp</h2>"
                + "<p>Xin chào <b>" + escape(name != null && !name.isBlank() ? name : "bạn") + "</b>,</p>"
                + "<p>Tài khoản của bạn vừa yêu cầu xuất <b>báo cáo tổng hợp</b> (thông tin khách hàng, doanh thu, "
                + "tồn kho, phiếu dịch vụ, biên lợi nhuận). Nhập mã sau vào màn hình xuất báo cáo:</p>"
                + "<p style=\"font-size:30px;font-weight:bold;letter-spacing:8px;margin:18px 0\">" + escape(code) + "</p>"
                + "<p>Mã có hiệu lực trong " + CODE_TTL.toMinutes() + " phút và chỉ dùng được cho tài khoản của bạn. "
                + "Sau khi xác minh, bạn có " + GRANT_TTL.toMinutes() + " phút để xuất file.</p>"
                + "<p style=\"color:#667085;font-size:13px\">Thời điểm yêu cầu: " + LocalDateTime.now().format(TIME)
                + (ip != null && !ip.isBlank() ? " · Địa chỉ IP: " + escape(ip) : "") + "</p>"
                + "<p style=\"color:#b91c1c;font-size:13px\">Nếu không phải bạn yêu cầu: KHÔNG đưa mã này cho ai, "
                + "đổi mật khẩu và báo ngay cho quản trị viên — có thể tài khoản đang bị người khác sử dụng.</p>"
                + "</div>";
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
