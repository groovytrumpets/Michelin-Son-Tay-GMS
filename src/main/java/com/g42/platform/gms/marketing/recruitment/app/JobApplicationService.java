package com.g42.platform.gms.marketing.recruitment.app;

import com.g42.platform.gms.marketing.recruitment.api.dto.RecruitmentDtos;
import com.g42.platform.gms.marketing.recruitment.domain.ApplicationStatus;
import com.g42.platform.gms.marketing.recruitment.domain.NotifyStatus;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobApplicationJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobPositionJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.repository.JobApplicationJpaRepo;
import com.g42.platform.gms.marketing.recruitment.infrastructure.repository.JobPositionJpaRepo;
import com.g42.platform.gms.marketing.recruitment.infrastructure.specification.JobApplicationSpecification;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

/** Nhận hồ sơ ứng tuyển từ trang khách và phục vụ màn hình xử lý hồ sơ. */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationService {

    /** Cùng số điện thoại nộp lại cùng vị trí trong khoảng này thì coi là bấm nhầm. */
    private static final int DUPLICATE_WINDOW_HOURS = 12;

    private static final DateTimeFormatter CODE_PERIOD = DateTimeFormatter.ofPattern("yyMM");

    private final JobApplicationJpaRepo applicationRepo;
    private final JobPositionJpaRepo jobRepo;
    private final RecruitmentMapper mapper;
    private final RecruitmentJsonCodec jsonCodec;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    // ------------------------------------------------------------- công khai

    /**
     * Ghi nhận một hồ sơ mới.
     *
     * <p>Thư báo về hộp thư quản lý KHÔNG gửi ở đây mà qua sự kiện chạy sau khi
     * giao dịch commit: máy chủ SMTP chậm hay chết không được phép làm ứng viên
     * mất công gõ lại toàn bộ biểu mẫu.
     */
    @Transactional
    public RecruitmentDtos.ApplicationReceiptDto submit(String jobSlug,
                                                        RecruitmentDtos.ApplicationSubmitRequest request,
                                                        String clientIp) {
        JobPositionJpa job = jobRepo.findBySlugAndDeletedAtIsNull(jobSlug)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tin tuyển dụng"));

        if (!job.isOpenForApplication()) {
            throw new IllegalStateException("Vị trí này đã ngừng nhận hồ sơ");
        }

        // Bẫy bot: ô ẩn mà người thật không nhìn thấy nên không bao giờ điền.
        // Trả về phiếu giả thay vì báo lỗi để bot không dò ra được luật chặn.
        if (request.website() != null && !request.website().isBlank()) {
            log.info("Tuyển dụng: bỏ qua một lượt gửi dính bẫy bot ở vị trí {}", jobSlug);
            return new RecruitmentDtos.ApplicationReceiptDto(
                    "—", job.getTitle(), LocalDateTime.now(), "Đã ghi nhận hồ sơ của bạn.");
        }

        String phone = request.phone().trim();
        LocalDateTime since = LocalDateTime.now().minusHours(DUPLICATE_WINDOW_HOURS);
        if (applicationRepo.existsByJob_JobIdAndPhoneAndCreatedAtGreaterThanEqual(job.getJobId(), phone, since)) {
            throw new IllegalStateException(
                    "Số điện thoại này vừa nộp hồ sơ cho vị trí trên. Nếu cần bổ sung, hãy gọi trực tiếp cho garage.");
        }

        JobApplicationJpa application = new JobApplicationJpa();
        application.setJob(job);
        application.setCode(nextCode());
        application.setFullName(request.fullName().trim());
        application.setPhone(phone);
        application.setEmail(trimToNull(request.email()));
        application.setDateOfBirth(request.dateOfBirth());
        application.setGender(trimToNull(request.gender()));
        application.setAddress(trimToNull(request.address()));
        application.setYearsExperience(request.yearsExperience());
        application.setCurrentPosition(trimToNull(request.currentPosition()));
        application.setExpectedSalary(request.expectedSalary());
        application.setAvailableFrom(request.availableFrom());
        application.setCoverLetter(trimToNull(request.coverLetter()));
        application.setCvUrl(trimToNull(request.cvUrl()));
        application.setPortfolioUrl(trimToNull(request.portfolioUrl()));
        application.setAnswersJson(jsonCodec.writeAnswers(
                request.answers(), jsonCodec.readFields(job.getFormFieldsJson())));
        application.setStatus(ApplicationStatus.NEW);
        application.setNotifyStatus(NotifyStatus.PENDING);
        application.setAckSent(false);
        application.setUtmSource(trimToNull(request.utmSource()));
        application.setUtmCampaign(trimToNull(request.utmCampaign()));
        application.setSubmitterHash(hashIp(clientIp));
        application.setCreatedAt(LocalDateTime.now());

        JobApplicationJpa saved = applicationRepo.save(application);
        jobRepo.incrementApplicationCount(job.getJobId());

        eventPublisher.publishEvent(new RecruitmentEvents.ApplicationSubmitted(saved.getApplicationId()));

        return new RecruitmentDtos.ApplicationReceiptDto(
                saved.getCode(),
                job.getTitle(),
                saved.getCreatedAt(),
                "Cảm ơn bạn! Hồ sơ đã được gửi tới bộ phận nhân sự. Vui lòng lưu lại mã hồ sơ để tiện đối chiếu.");
    }

    // ------------------------------------------------------------- quản trị

    @Transactional(readOnly = true)
    public Page<RecruitmentDtos.ApplicationSummaryDto> search(ApplicationStatus status, Long jobId,
                                                              LocalDateTime from, LocalDateTime to,
                                                              String keyword, Pageable pageable) {
        Specification<JobApplicationJpa> spec = Specification
                .<JobApplicationJpa>where(JobApplicationSpecification.hasStatus(status))
                .and(JobApplicationSpecification.hasJob(jobId))
                .and(JobApplicationSpecification.createdFrom(from))
                .and(JobApplicationSpecification.createdTo(to))
                .and(JobApplicationSpecification.keyword(keyword));
        return applicationRepo.findAll(spec, pageable).map(mapper::toApplicationSummary);
    }

    @Transactional(readOnly = true)
    public RecruitmentDtos.ApplicationDetailDto getDetail(Long applicationId) {
        return mapper.toApplicationDetail(require(applicationId));
    }

    @Transactional
    public RecruitmentDtos.ApplicationDetailDto update(Long applicationId,
                                                       RecruitmentDtos.ApplicationUpdateRequest request,
                                                       Integer staffId) {
        JobApplicationJpa application = require(applicationId);

        if (request.status() != null) application.setStatus(request.status());
        if (request.rating() != null) {
            int rating = Math.max(0, Math.min(5, request.rating()));
            application.setRating(rating == 0 ? null : rating);
        }
        if (request.internalNote() != null) application.setInternalNote(trimToNull(request.internalNote()));

        application.setHandledBy(staffId == null ? null : entityManager.getReference(StaffProfileJpa.class, staffId));
        application.setHandledAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());

        return mapper.toApplicationDetail(applicationRepo.save(application));
    }

    @Transactional
    public void delete(Long applicationId) {
        applicationRepo.delete(require(applicationId));
    }

    // ------------------------------------------------------------- nội bộ

    private JobApplicationJpa require(Long applicationId) {
        return applicationRepo.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hồ sơ " + applicationId));
    }

    /**
     * Sinh mã hồ sơ dạng UT2608-0007 (UT + năm/tháng + số thứ tự trong tháng).
     *
     * <p>Đếm lại theo tháng thay vì dùng thẳng id: mã ngắn, đọc qua điện thoại
     * được, và nhìn vào là biết hồ sơ của đợt nào. Vòng lặp phòng trường hợp
     * hai hồ sơ vào cùng lúc cùng lấy được một số — khoá duy nhất trên cột code
     * mới là chỗ bảo đảm cuối cùng.
     */
    private String nextCode() {
        YearMonth month = YearMonth.now();
        LocalDateTime from = month.atDay(1).atStartOfDay();
        LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
        long seq = applicationRepo.countInPeriod(from, to) + 1;

        for (int attempt = 0; attempt < 50; attempt++) {
            String candidate = "UT" + month.format(CODE_PERIOD) + "-" + String.format("%04d", seq + attempt);
            if (!applicationRepo.existsByCode(candidate)) return candidate;
        }
        return "UT" + month.format(CODE_PERIOD) + "-" + System.currentTimeMillis() % 100000;
    }

    /**
     * Băm địa chỉ IP thay vì lưu thô — hồ sơ ứng tuyển đã là dữ liệu cá nhân,
     * không cần giữ thêm thứ định danh được đường truyền của người gửi.
     */
    private String hashIp(String ip) {
        if (ip == null || ip.isBlank()) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(("gms-recruitment:" + ip).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
