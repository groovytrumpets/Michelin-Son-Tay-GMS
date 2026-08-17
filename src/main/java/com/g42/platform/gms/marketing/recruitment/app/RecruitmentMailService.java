package com.g42.platform.gms.marketing.recruitment.app;

import com.g42.platform.gms.marketing.recruitment.domain.NotifyStatus;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobApplicationJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobPositionJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.RecruitmentSettingJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.repository.JobApplicationJpaRepo;
import com.g42.platform.gms.notification.infrastructure.EmailNotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Soạn và gửi thư của phân hệ tuyển dụng.
 *
 * <p>Gửi thư nằm ngoài giao dịch lưu hồ sơ, chạy sau khi giao dịch đã commit:
 * máy chủ SMTP chậm hoặc chết không được phép làm ứng viên mất hồ sơ vừa gõ.
 * Đổi lại, kết quả gửi phải được ghi lại vào chính bản ghi hồ sơ
 * ({@code notify_status}) để người tuyển nhìn thấy trên màn hình danh sách khi
 * thư không tới nơi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitmentMailService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JobApplicationJpaRepo applicationRepo;
    private final RecruitmentSettingService settingService;
    private final RecruitmentJsonCodec jsonCodec;
    private final EmailNotificationSender emailSender;

    /**
     * Gửi thư báo hồ sơ mới tới hộp thư quản lý, rồi ghi lại kết quả.
     *
     * <p>Tách thành hàm công khai để màn hình quản trị gọi lại được khi lần gửi
     * đầu hỏng (vd lúc đó chưa điền mật khẩu SMTP).
     */
    @Transactional
    public NotifyStatus notifyManagers(Long applicationId) {
        JobApplicationJpa application = applicationRepo.findById(applicationId).orElse(null);
        if (application == null) {
            log.warn("Tuyển dụng: không tìm thấy hồ sơ {} để gửi thư", applicationId);
            return NotifyStatus.FAILED;
        }

        RecruitmentSettingJpa setting = settingService.getOrCreate();
        JobPositionJpa job = application.getJob();

        if (!Boolean.TRUE.equals(setting.getNotifyEnabled())) {
            return markNotified(application, NotifyStatus.SKIPPED, "Thông báo email đang tắt trong cấu hình");
        }

        // Hộp thư riêng của vị trí được ưu tiên: tin tuyển kỹ thuật viên và tin
        // tuyển kế toán thường do hai người khác nhau theo dõi.
        List<String> recipients = settingService.splitEmails(job == null ? null : job.getNotifyEmails());
        if (recipients.isEmpty()) recipients = settingService.splitEmails(setting.getRecipientEmails());

        if (recipients.isEmpty()) {
            return markNotified(application, NotifyStatus.SKIPPED, "Chưa cấu hình email nhận hồ sơ");
        }

        try {
            emailSender.sendHtmlOrThrow(
                    recipients,
                    settingService.splitEmails(setting.getCcEmails()),
                    settingService.prefixSubject(setting, buildSubject(application, job)),
                    buildManagerBody(application, job));
            return markNotified(application, NotifyStatus.SENT, null);
        } catch (EmailNotificationSender.MailDeliveryException e) {
            return markNotified(application, NotifyStatus.FAILED, e.getMessage());
        }
    }

    /**
     * Thư cảm ơn gửi về địa chỉ của chính ứng viên. Tách khỏi thư báo quản lý
     * vì hai thư có thể hỏng độc lập: quản lý vẫn phải nhận được hồ sơ ngay cả
     * khi ứng viên gõ sai email của mình.
     */
    @Transactional
    public void sendAcknowledgement(Long applicationId) {
        JobApplicationJpa application = applicationRepo.findById(applicationId).orElse(null);
        if (application == null) return;

        RecruitmentSettingJpa setting = settingService.getOrCreate();
        if (!Boolean.TRUE.equals(setting.getSendAckEnabled())) return;
        if (application.getEmail() == null || application.getEmail().isBlank()) return;

        JobPositionJpa job = application.getJob();
        String subject = setting.getAckSubject() == null || setting.getAckSubject().isBlank()
                ? "Chúng tôi đã nhận hồ sơ của bạn"
                : setting.getAckSubject();
        String body = fillTemplate(setting.getAckBodyHtml(), application, job);

        try {
            emailSender.sendHtmlOrThrow(List.of(application.getEmail()), List.of(), subject, body);
            application.setAckSent(true);
            applicationRepo.save(application);
        } catch (EmailNotificationSender.MailDeliveryException e) {
            log.warn("Tuyển dụng: không gửi được thư cảm ơn tới {}: {}", application.getEmail(), e.getMessage());
        }
    }

    // ------------------------------------------------------------- soạn thư

    private String buildSubject(JobApplicationJpa application, JobPositionJpa job) {
        String position = job == null ? "vị trí đã gỡ" : job.getTitle();
        return "Hồ sơ mới: " + application.getFullName() + " — " + position + " (" + application.getCode() + ")";
    }

    /**
     * Thân thư báo quản lý: bảng thông tin ứng viên đầy đủ.
     *
     * <p>Dựng bảng HTML chứ không chỉ gửi một dòng "có hồ sơ mới, vào xem" —
     * quản lý thường đọc thư trên điện thoại và cần đủ dữ liệu để gọi luôn cho
     * ứng viên mà không phải mở khu quản trị.
     */
    private String buildManagerBody(JobApplicationJpa application, JobPositionJpa job) {
        Map<String, String> rows = new LinkedHashMap<>();
        rows.put("Mã hồ sơ", application.getCode());
        rows.put("Vị trí ứng tuyển", job == null ? "—" : job.getTitle());
        rows.put("Họ tên", application.getFullName());
        rows.put("Điện thoại", application.getPhone());
        rows.put("Email", orDash(application.getEmail()));
        rows.put("Ngày sinh", application.getDateOfBirth() == null ? "—"
                : application.getDateOfBirth().format(DATE_FORMATTER));
        rows.put("Giới tính", orDash(application.getGender()));
        rows.put("Địa chỉ", orDash(application.getAddress()));
        rows.put("Số năm kinh nghiệm", application.getYearsExperience() == null ? "—"
                : application.getYearsExperience() + " năm");
        rows.put("Công việc hiện tại", orDash(application.getCurrentPosition()));
        rows.put("Mức lương mong muốn", application.getExpectedSalary() == null ? "—"
                : String.format("%,.0f đ", application.getExpectedSalary()));
        rows.put("Có thể đi làm từ", application.getAvailableFrom() == null ? "—"
                : application.getAvailableFrom().format(DATE_FORMATTER));
        rows.put("Gửi lúc", application.getCreatedAt() == null ? "—"
                : application.getCreatedAt().format(TIME_FORMATTER));

        // Câu hỏi thêm: ghép nhãn câu hỏi với câu trả lời để thư đọc được ngay,
        // thay vì hiện khoá kỹ thuật kiểu "so-nam-lai-xe".
        Map<String, String> answers = jsonCodec.readAnswers(application.getAnswersJson());
        if (!answers.isEmpty() && job != null) {
            jsonCodec.readFields(job.getFormFieldsJson()).forEach(field -> {
                String answer = answers.get(field.key());
                if (answer != null && !answer.isBlank()) rows.put(field.label(), answer);
            });
        }

        StringBuilder html = new StringBuilder();
        html.append("<p>Có một hồ sơ ứng tuyển mới vừa gửi qua trang tuyển dụng.</p>");
        html.append("<table style=\"border-collapse:collapse;font-size:14px;\">");
        rows.forEach((label, value) -> html
                .append("<tr>")
                .append("<td style=\"padding:6px 12px 6px 0;color:#6b7382;vertical-align:top;\">")
                .append(escape(label)).append("</td>")
                .append("<td style=\"padding:6px 0;font-weight:600;\">")
                .append(escape(value)).append("</td>")
                .append("</tr>"));
        html.append("</table>");

        List<String> links = new ArrayList<>();
        if (application.getCvUrl() != null && !application.getCvUrl().isBlank()) {
            links.add("<a href=\"" + escape(application.getCvUrl()) + "\">Tải CV đính kèm</a>");
        }
        if (application.getPortfolioUrl() != null && !application.getPortfolioUrl().isBlank()) {
            links.add("<a href=\"" + escape(application.getPortfolioUrl()) + "\">Hồ sơ năng lực / liên kết</a>");
        }
        if (!links.isEmpty()) {
            html.append("<p>").append(String.join(" &nbsp;·&nbsp; ", links)).append("</p>");
        }

        if (application.getCoverLetter() != null && !application.getCoverLetter().isBlank()) {
            html.append("<p style=\"color:#6b7382;margin-bottom:4px;\">Thư giới thiệu:</p>")
                    .append("<blockquote style=\"margin:0;padding:10px 14px;border-left:3px solid #d8dce3;white-space:pre-wrap;\">")
                    .append(escape(application.getCoverLetter()))
                    .append("</blockquote>");
        }

        html.append("<p style=\"color:#6b7382;font-size:13px;\">")
                .append("Mở khu quản trị &gt; Tuyển dụng &gt; Hồ sơ ứng tuyển để đổi trạng thái và ghi chú.")
                .append("</p>");
        return html.toString();
    }

    /** Thay biến trong mẫu thư cảm ơn do quản lý tự soạn. */
    private String fillTemplate(String template, JobApplicationJpa application, JobPositionJpa job) {
        String body = (template == null || template.isBlank())
                ? "<p>Chào {{ten}},</p><p>Chúng tôi đã nhận hồ sơ ứng tuyển vị trí <b>{{vitri}}</b> (mã hồ sơ {{mahoso}}).</p>"
                : template;
        return body
                .replace("{{ten}}", escape(application.getFullName()))
                .replace("{{vitri}}", escape(job == null ? "" : job.getTitle()))
                .replace("{{mahoso}}", escape(application.getCode()));
    }

    private NotifyStatus markNotified(JobApplicationJpa application, NotifyStatus status, String error) {
        application.setNotifyStatus(status);
        application.setNotifyError(error == null ? null : truncate(error, 500));
        application.setNotifiedAt(LocalDateTime.now());
        applicationRepo.save(application);
        return status;
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
