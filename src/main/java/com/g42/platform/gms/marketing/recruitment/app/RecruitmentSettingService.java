package com.g42.platform.gms.marketing.recruitment.app;

import com.g42.platform.gms.marketing.news.app.PostContentService;
import com.g42.platform.gms.marketing.recruitment.api.dto.RecruitmentDtos;
import com.g42.platform.gms.marketing.recruitment.domain.JobStatus;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.RecruitmentSettingJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.repository.JobPositionJpaRepo;
import com.g42.platform.gms.marketing.recruitment.infrastructure.repository.RecruitmentSettingJpaRepo;
import com.g42.platform.gms.notification.infrastructure.EmailNotificationSender;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Cấu hình dùng chung của phân hệ tuyển dụng, quan trọng nhất là DANH SÁCH
 * EMAIL NHẬN hồ sơ.
 *
 * <p>Bảng chỉ có một dòng. Nếu vì lý do nào đó dòng seed biến mất (khôi phục DB
 * từ bản dump cũ hơn changeset 016 chẳng hạn), hàm {@link #getOrCreate()} tự
 * dựng lại dòng mặc định thay vì để cả trang tuyển dụng chết vì thiếu cấu hình.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitmentSettingService {

    /**
     * Kiểm tra email ở mức "có đúng hình dạng địa chỉ hay không".
     *
     * <p>Không cố bắt mọi trường hợp của RFC 5322 — mục đích chỉ là chặn lỗi gõ
     * nhầm ngay trên màn hình cấu hình, còn đúng/sai thật thì chỉ máy chủ SMTP
     * mới trả lời được.
     */
    private static final Pattern EMAIL_SHAPE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    /** Chặn dán nhầm cả danh bạ vào ô người nhận. */
    private static final int MAX_RECIPIENTS = 10;

    private final RecruitmentSettingJpaRepo settingRepo;
    private final JobPositionJpaRepo jobRepo;
    private final RecruitmentMapper mapper;
    private final PostContentService contentService;
    private final EmailNotificationSender emailSender;
    private final EntityManager entityManager;

    @Transactional
    public RecruitmentSettingJpa getOrCreate() {
        return settingRepo.findFirstByOrderBySettingIdAsc().orElseGet(() -> {
            log.warn("Tuyển dụng: chưa có dòng recruitment_setting, dựng lại bản mặc định");
            RecruitmentSettingJpa fresh = new RecruitmentSettingJpa();
            fresh.setIsPageEnabled(true);
            fresh.setPageTitle("Tuyển dụng tại Michelin Sơn Tây");
            fresh.setNotifyEnabled(true);
            fresh.setSubjectPrefix("[Ứng tuyển]");
            fresh.setSendAckEnabled(false);
            fresh.setUpdatedAt(LocalDateTime.now());
            return settingRepo.save(fresh);
        });
    }

    @Transactional
    public RecruitmentDtos.SettingDto getForAdmin() {
        return mapper.toSettingDto(getOrCreate(), emailSender.isConfigured());
    }

    @Transactional
    public RecruitmentDtos.PublicPageDto getPublicPage() {
        List<String> departments = jobRepo.findDistinctDepartments(
                List.of(JobStatus.PUBLISHED, JobStatus.CLOSED));
        return mapper.toPublicPageDto(getOrCreate(), departments);
    }

    @Transactional
    public RecruitmentDtos.SettingDto save(RecruitmentDtos.SettingDto request, Integer staffId) {
        RecruitmentSettingJpa setting = getOrCreate();

        setting.setIsPageEnabled(request.isPageEnabled() == null || request.isPageEnabled());
        setting.setPageTitle(trimToNull(request.pageTitle()));
        setting.setPageSubtitle(trimToNull(request.pageSubtitle()));
        setting.setPageIntroHtml(contentService.sanitize(request.pageIntroHtml()));
        setting.setBannerUrl(trimToNull(request.bannerUrl()));
        setting.setContactHotline(trimToNull(request.contactHotline()));
        setting.setContactAddress(trimToNull(request.contactAddress()));

        setting.setNotifyEnabled(request.notifyEnabled() == null || request.notifyEnabled());
        setting.setRecipientEmails(normalizeEmailList(request.recipientEmails(), "Email nhận hồ sơ"));
        setting.setCcEmails(normalizeEmailList(request.ccEmails(), "Email nhận bản sao"));
        setting.setSubjectPrefix(trimToNull(request.subjectPrefix()));

        setting.setSendAckEnabled(Boolean.TRUE.equals(request.sendAckEnabled()));
        setting.setAckSubject(trimToNull(request.ackSubject()));
        setting.setAckBodyHtml(contentService.sanitize(request.ackBodyHtml()));

        setting.setSeoTitle(trimToNull(request.seoTitle()));
        setting.setSeoDescription(trimToNull(request.seoDescription()));
        setting.setOgImageUrl(trimToNull(request.ogImageUrl()));

        setting.setUpdatedAt(LocalDateTime.now());
        setting.setUpdatedBy(staffId == null ? null : entityManager.getReference(StaffProfileJpa.class, staffId));

        return mapper.toSettingDto(settingRepo.save(setting), emailSender.isConfigured());
    }

    /**
     * Gửi một thư thử tới địa chỉ chỉ định (bỏ trống thì gửi tới đúng danh sách
     * đang cấu hình). Đây là cách duy nhất để biết SMTP + danh sách người nhận
     * có chạy hay không mà không phải chờ một ứng viên thật gửi hồ sơ.
     */
    @Transactional(readOnly = true)
    public void sendTestMail(String explicitTo) {
        RecruitmentSettingJpa setting = settingRepo.findFirstByOrderBySettingIdAsc()
                .orElseThrow(() -> new IllegalStateException("Chưa có cấu hình tuyển dụng"));

        List<String> recipients = (explicitTo != null && !explicitTo.isBlank())
                ? List.of(explicitTo.trim())
                : splitEmails(setting.getRecipientEmails());

        if (recipients.isEmpty()) {
            throw new IllegalArgumentException(
                    "Chưa có địa chỉ nào để gửi thử. Điền ô \"Email nhận hồ sơ\" rồi lưu, hoặc nhập một địa chỉ ở ô gửi thử.");
        }

        String html = """
                <p>Đây là thư kiểm tra từ phân hệ tuyển dụng của Michelin Sơn Tây.</p>
                <p>Bạn nhận được thư này nghĩa là cấu hình máy chủ gửi thư và danh sách hộp thư nhận
                hồ sơ đã hoạt động. Từ giờ mỗi khi có ứng viên nộp hồ sơ, một thư tương tự kèm đầy đủ
                thông tin ứng viên sẽ được gửi tới đây.</p>
                <p style="color:#6b7382;font-size:13px;">Thư tự động, vui lòng không trả lời.</p>
                """;

        emailSender.sendHtmlOrThrow(
                recipients,
                List.of(),
                prefixSubject(setting, "Thư kiểm tra cấu hình nhận hồ sơ"),
                html);
    }

    // ------------------------------------------------------------- tiện ích

    /** Tiêu đề thư kèm tiền tố đã cấu hình, để quản lý đặt được bộ lọc trong hộp thư. */
    public String prefixSubject(RecruitmentSettingJpa setting, String subject) {
        String prefix = setting == null ? null : trimToNull(setting.getSubjectPrefix());
        return prefix == null ? subject : prefix + " " + subject;
    }

    /** Tách chuỗi "a@x.com, b@y.com" thành danh sách địa chỉ đã bỏ khoảng trắng. */
    public List<String> splitEmails(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String part : raw.split("[,;\\s]+")) {
            String email = part.trim();
            if (!email.isEmpty() && !result.contains(email)) result.add(email);
        }
        return result;
    }

    private String normalizeEmailList(String raw, String fieldLabel) {
        List<String> emails = splitEmails(raw);
        if (emails.isEmpty()) return null;
        if (emails.size() > MAX_RECIPIENTS) {
            throw new IllegalArgumentException(fieldLabel + ": tối đa " + MAX_RECIPIENTS + " địa chỉ");
        }
        for (String email : emails) {
            if (!EMAIL_SHAPE.matcher(email).matches()) {
                throw new IllegalArgumentException(fieldLabel + ": \"" + email + "\" không phải địa chỉ email hợp lệ");
            }
        }
        return String.join(", ", emails);
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
