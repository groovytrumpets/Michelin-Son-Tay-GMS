package com.g42.platform.gms.notification.infrastructure;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Kênh gửi thông báo qua email, thay thế cho Zalo ZNS khi khách hàng chọn EMAIL
 * làm kênh nhận thông báo, hoặc khi Zalo gửi thất bại (xem CustomerNotificationDispatcher).
 *
 * Nếu chưa cấu hình SMTP (notification.email.host rỗng), mọi lời gọi send* sẽ
 * bị bỏ qua (log warn) thay vì ném lỗi — giữ nguyên tinh thần "tắt êm" giống
 * ZaloNotificationSender khi thiếu access token. Khi đó hàm trả về false để
 * dispatcher biết mà thử kênh còn lại.
 */
@Slf4j
@Component
public class EmailNotificationSender {

    private final JavaMailSender mailSender;

    @Value("${notification.email.host:}")
    private String smtpHost;

    @Value("${notification.email.password:}")
    private String smtpPassword;

    @Value("${notification.email.from}")
    private String fromAddress;

    @Value("${notification.email.from-name}")
    private String fromName;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public EmailNotificationSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean sendBookingConfirm(String to, String customerName, List<String> productName,
                                    String orderCode, LocalDateTime bookingTime, String garageLocation) {
        String services = (productName == null || productName.isEmpty())
                ? "Không có dịch vụ cụ thể" : String.join(", ", productName);
        String html = "<p>Xin chào <b>" + escape(customerName) + "</b>,</p>"
                + "<p>Lịch hẹn của bạn đã được xác nhận:</p>"
                + "<ul>"
                + "<li>Mã đặt lịch: <b>" + escape(orderCode) + "</b></li>"
                + "<li>Dịch vụ: " + escape(services) + "</li>"
                + "<li>Thời gian: " + bookingTime.format(TIME_FORMATTER) + "</li>"
                + "<li>Địa điểm: " + escape(garageLocation) + "</li>"
                + "</ul>"
                + "<p>Hẹn gặp bạn tại xưởng!</p>";
        return send(to, "Xác nhận lịch hẹn " + orderCode, html);
    }

    public boolean sendOtpVerify(String to, String otp) {
        String html = "<p>Mã xác thực (OTP) của bạn là:</p>"
                + "<p style=\"font-size:24px;font-weight:bold;letter-spacing:4px;\">" + escape(otp) + "</p>"
                + "<p>Mã có hiệu lực trong 5 phút. Không chia sẻ mã này với bất kỳ ai.</p>";
        return send(to, "Mã xác thực OTP", html);
    }

    public boolean sendFeedback(String to, String name, String code) {
        String html = "<p>Xin chào <b>" + escape(name) + "</b>,</p>"
                + "<p>Cảm ơn bạn đã sử dụng dịch vụ tại Michelin Sơn Tây (mã phiếu: <b>" + escape(code) + "</b>).</p>"
                + "<p>Rất mong nhận được đánh giá của bạn để chúng tôi phục vụ tốt hơn.</p>";
        return send(to, "Cảm ơn quý khách - " + code, html);
    }

    public boolean sendEstimate(String to, String customerName, List<String> productName, String orderCode,
                              LocalDateTime createAt, String garageLocation, String totalPrice) {
        String services = (productName == null || productName.isEmpty())
                ? "Không có dịch vụ cụ thể" : String.join(", ", productName);
        String html = "<p>Xin chào <b>" + escape(customerName) + "</b>,</p>"
                + "<p>Báo giá cho phiếu <b>" + escape(orderCode) + "</b> đã sẵn sàng:</p>"
                + "<ul>"
                + "<li>Dịch vụ: " + escape(services) + "</li>"
                + "<li>Thời gian: " + (createAt != null ? createAt.format(TIME_FORMATTER) : "") + "</li>"
                + "<li>Địa điểm: " + escape(garageLocation) + "</li>"
                + "<li>Tổng tiền tạm tính: <b>" + escape(totalPrice) + "</b></li>"
                + "</ul>";
        return send(to, "Báo giá dịch vụ " + orderCode, html);
    }

    /** @return true nếu email đã được gửi đi; false nếu thiếu người nhận, chưa cấu hình SMTP, hoặc gửi lỗi. */
    private boolean send(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            log.warn("Email: bỏ qua gửi '{}' vì thiếu địa chỉ người nhận", subject);
            return false;
        }
        if (smtpHost == null || smtpHost.isBlank()) {
            log.warn("Email: chưa cấu hình SMTP (notification.email.host trống), bỏ qua gửi '{}' tới {}", subject, to);
            return false;
        }
        if (smtpPassword == null || smtpPassword.isBlank()) {
            log.warn("Email: chưa có mật khẩu SMTP (đặt biến môi trường MAIL_SMTP_PASSWORD)," +
                    " bỏ qua gửi '{}' tới {}", subject, to);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.debug("Email: gửi '{}' tới {} thành công", subject, to);
            return true;
        } catch (Exception e) {
            // Kèm nguyên nhân gốc vì lớp ngoài chỉ nói chung chung ("Authentication failed"),
            // còn thông điệp thật của máy chủ SMTP mới cho biết sai ở đâu
            // (vd Gmail: "535-5.7.8 Username and Password not accepted").
            log.error("Email: gửi '{}' tới {} thất bại: {}", subject, to, describeFailure(e));
            return false;
        }
    }

    private String describeFailure(Throwable error) {
        StringBuilder message = new StringBuilder(String.valueOf(error.getMessage()));
        Throwable cause = error.getCause();
        while (cause != null && cause != cause.getCause()) {
            message.append(" | ").append(cause.getClass().getSimpleName())
                    .append(": ").append(cause.getMessage());
            cause = cause.getCause();
        }
        return message.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
