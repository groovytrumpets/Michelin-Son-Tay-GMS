package com.g42.platform.gms.marketing.recruitment.app;

import com.g42.platform.gms.marketing.news.app.PostContentService;
import com.g42.platform.gms.marketing.news.app.SlugGenerator;
import com.g42.platform.gms.marketing.recruitment.api.dto.RecruitmentDtos;
import com.g42.platform.gms.marketing.recruitment.domain.ApplicationStatus;
import com.g42.platform.gms.marketing.recruitment.domain.EmploymentType;
import com.g42.platform.gms.marketing.recruitment.domain.JobStatus;
import com.g42.platform.gms.marketing.recruitment.domain.SalaryPeriod;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobBenefitJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobPositionJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.repository.JobApplicationJpaRepo;
import com.g42.platform.gms.marketing.recruitment.infrastructure.repository.JobPositionJpaRepo;
import com.g42.platform.gms.marketing.recruitment.infrastructure.specification.JobPositionSpecification;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** Soạn, duyệt và hiển thị tin tuyển dụng. */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobPositionService {

    private static final int MAX_BENEFITS = 20;

    private final JobPositionJpaRepo jobRepo;
    private final JobApplicationJpaRepo applicationRepo;
    private final RecruitmentMapper mapper;
    private final RecruitmentJsonCodec jsonCodec;
    private final PostContentService contentService;
    private final EntityManager entityManager;

    // ------------------------------------------------------------- quản trị

    @Transactional(readOnly = true)
    public Page<RecruitmentDtos.JobSummaryDto> searchForAdmin(JobStatus status, String department,
                                                              EmploymentType type, String keyword,
                                                              Pageable pageable) {
        Specification<JobPositionJpa> spec = JobPositionSpecification.notDeleted()
                .and(JobPositionSpecification.hasStatus(status))
                .and(JobPositionSpecification.hasDepartment(department))
                .and(JobPositionSpecification.hasEmploymentType(type))
                .and(JobPositionSpecification.keyword(keyword));
        return jobRepo.findAll(spec, pageable).map(mapper::toSummary);
    }

    @Transactional(readOnly = true)
    public RecruitmentDtos.JobAdminDetailDto getForEdit(Long jobId) {
        return mapper.toAdminDetail(requireJob(jobId));
    }

    @Transactional
    public RecruitmentDtos.JobAdminDetailDto create(RecruitmentDtos.JobSaveRequest request, Integer staffId) {
        JobPositionJpa job = new JobPositionJpa();
        job.setStatus(JobStatus.DRAFT);
        job.setViewCount(0L);
        job.setApplicationCount(0);
        job.setCreatedAt(LocalDateTime.now());
        job.setAuthor(staffId == null ? null : entityManager.getReference(StaffProfileJpa.class, staffId));
        job.setSlug(resolveSlug(request.slug(), request.title(), null));

        applyRequest(job, request);
        return mapper.toAdminDetail(jobRepo.save(job));
    }

    @Transactional
    public RecruitmentDtos.JobAdminDetailDto update(Long jobId, RecruitmentDtos.JobSaveRequest request) {
        JobPositionJpa job = requireJob(jobId);
        job.setSlug(resolveSlug(request.slug(), request.title(), job));
        applyRequest(job, request);
        return mapper.toAdminDetail(jobRepo.save(job));
    }

    /**
     * Đổi trạng thái tin.
     *
     * <p>{@code publishedAt} chỉ được đặt ở lần đăng ĐẦU TIÊN: đóng rồi mở lại
     * một tin không phải là đăng tin mới, nên ngày đăng gốc phải giữ nguyên để
     * thứ tự ngoài trang danh sách không bị xáo mỗi lần biên tập bấm nút.
     */
    @Transactional
    public RecruitmentDtos.JobAdminDetailDto changeStatus(Long jobId,
                                                         RecruitmentDtos.JobStatusChangeRequest request,
                                                         Integer staffId) {
        JobPositionJpa job = requireJob(jobId);
        JobStatus target = request.status();
        if (target == null) throw new IllegalArgumentException("Thiếu trạng thái cần chuyển");

        job.setStatus(target);
        job.setReviewNote(request.reviewNote());
        if (target == JobStatus.PUBLISHED && job.getPublishedAt() == null) {
            job.setPublishedAt(LocalDateTime.now());
        }
        if (target == JobStatus.PUBLISHED || target == JobStatus.DRAFT) {
            job.setReviewer(staffId == null ? null : entityManager.getReference(StaffProfileJpa.class, staffId));
        }
        job.setUpdatedAt(LocalDateTime.now());
        return mapper.toAdminDetail(jobRepo.save(job));
    }

    /**
     * Xoá mềm. Hồ sơ ứng viên đã nộp vẫn trỏ về tin này, nên xoá cứng sẽ kéo
     * theo cả hồ sơ (khoá ngoại CASCADE) — mất dữ liệu người thật đã gửi.
     */
    @Transactional
    public void softDelete(Long jobId) {
        JobPositionJpa job = requireJob(jobId);
        job.setDeletedAt(LocalDateTime.now());
        jobRepo.save(job);
    }

    @Transactional(readOnly = true)
    public RecruitmentDtos.StatsDto stats() {
        LocalDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay();
        return new RecruitmentDtos.StatsDto(
                jobRepo.countByStatusAndDeletedAtIsNull(JobStatus.DRAFT),
                jobRepo.countByStatusAndDeletedAtIsNull(JobStatus.PENDING),
                jobRepo.countByStatusAndDeletedAtIsNull(JobStatus.PUBLISHED),
                jobRepo.countByStatusAndDeletedAtIsNull(JobStatus.CLOSED),
                applicationRepo.countByStatus(ApplicationStatus.NEW),
                applicationRepo.countByCreatedAtGreaterThanEqual(monthStart),
                applicationRepo.count());
    }

    // ------------------------------------------------------------- công khai

    @Transactional(readOnly = true)
    public Page<RecruitmentDtos.JobSummaryDto> searchForPublic(String department, EmploymentType type,
                                                               String keyword, Pageable pageable) {
        Specification<JobPositionJpa> spec = JobPositionSpecification.notDeleted()
                .and(JobPositionSpecification.publiclyListed())
                .and(JobPositionSpecification.hasDepartment(department))
                .and(JobPositionSpecification.hasEmploymentType(type))
                .and(JobPositionSpecification.keyword(keyword))
                .and(JobPositionSpecification.defaultPublicOrder());
        return jobRepo.findAll(spec, pageable).map(mapper::toSummary);
    }

    /**
     * Chi tiết cho trang khách. Tin ARCHIVED vẫn mở được bằng đường dẫn trực
     * tiếp — link đã dán lên nhóm nghề hay Facebook không nên biến thành 404.
     */
    @Transactional
    public RecruitmentDtos.JobDetailDto getPublicDetail(String slug) {
        JobPositionJpa job = jobRepo.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tin tuyển dụng"));
        if (job.getStatus() == JobStatus.DRAFT || job.getStatus() == JobStatus.PENDING) {
            throw new EntityNotFoundException("Không tìm thấy tin tuyển dụng");
        }
        jobRepo.incrementViewCount(job.getJobId());
        return mapper.toPublicDetail(job);
    }

    /** Tin dùng cho thẻ meta khi bot mạng xã hội quét đường dẫn chia sẻ. */
    @Transactional(readOnly = true)
    public JobPositionJpa findPublicEntity(String slug) {
        return jobRepo.findBySlugAndDeletedAtIsNull(slug)
                .filter(JobPositionJpa::isVisibleToPublic)
                .orElse(null);
    }

    // ------------------------------------------------------------- nội bộ

    JobPositionJpa requireJob(Long jobId) {
        return jobRepo.findByJobIdAndDeletedAtIsNull(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tin tuyển dụng " + jobId));
    }

    private void applyRequest(JobPositionJpa job, RecruitmentDtos.JobSaveRequest request) {
        job.setTitle(request.title().trim());
        job.setDepartment(trimToNull(request.department()));
        job.setEmploymentType(request.employmentType() == null ? EmploymentType.FULL_TIME : request.employmentType());
        job.setWorkLocation(trimToNull(request.workLocation()));
        job.setHeadcount(request.headcount() == null || request.headcount() < 1 ? 1 : request.headcount());
        job.setExperienceText(trimToNull(request.experienceText()));

        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setSalaryPeriod(request.salaryPeriod() == null ? SalaryPeriod.MONTH : request.salaryPeriod());
        job.setSalaryText(trimToNull(request.salaryText()));

        job.setSummary(trimToNull(request.summary()));
        job.setContentHtml(contentService.sanitize(request.contentHtml()));
        job.setRequirementHtml(contentService.sanitize(request.requirementHtml()));
        job.setBenefitHtml(contentService.sanitize(request.benefitHtml()));

        job.setThumbnailUrl(trimToNull(request.thumbnailUrl()));
        job.setCoverUrl(trimToNull(request.coverUrl()));
        job.setIsFeatured(Boolean.TRUE.equals(request.isFeatured()));
        job.setIsUrgent(Boolean.TRUE.equals(request.isUrgent()));
        job.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        job.setClosingDate(request.closingDate());
        job.setNotifyEmails(trimToNull(request.notifyEmails()));

        job.setSeoTitle(trimToNull(request.seoTitle()));
        job.setSeoDescription(trimToNull(request.seoDescription()));
        job.setOgImageUrl(trimToNull(request.ogImageUrl()));
        job.setAllowIndex(request.allowIndex() == null || request.allowIndex());

        job.setFormFieldsJson(jsonCodec.writeFields(request.formFields()));
        replaceBenefits(job, request.benefits());

        job.setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Ghi lại toàn bộ danh sách mức hỗ trợ theo đúng thứ tự người dùng kéo thả.
     *
     * <p>Xoá sạch rồi thêm lại thay vì so từng dòng: danh sách rất ngắn (tối đa
     * {@value #MAX_BENEFITS} khoản) nên cách này rẻ, mà lại không có nguy cơ để
     * sót một dòng cũ không còn trong biểu mẫu.
     */
    private void replaceBenefits(JobPositionJpa job, List<RecruitmentDtos.BenefitDto> benefits) {
        job.getBenefits().clear();
        if (benefits == null) return;

        int order = 0;
        for (RecruitmentDtos.BenefitDto dto : benefits) {
            if (dto == null || dto.label() == null || dto.label().isBlank()) continue;
            if (job.getBenefits().size() >= MAX_BENEFITS) break;

            JobBenefitJpa benefit = new JobBenefitJpa();
            benefit.setJob(job);
            benefit.setLabel(dto.label().trim());
            benefit.setValueText(trimToNull(dto.valueText()));
            benefit.setIcon(trimToNull(dto.icon()));
            benefit.setDisplayOrder(order++);
            job.getBenefits().add(benefit);
        }
    }

    /**
     * Chốt slug. Người soạn có thể tự đặt; bỏ trống thì sinh từ tên vị trí.
     * Slug đang thuộc về chính tin này thì giữ nguyên, không thêm hậu tố số.
     */
    private String resolveSlug(String requestedSlug, String title, JobPositionJpa current) {
        String source = (requestedSlug != null && !requestedSlug.isBlank()) ? requestedSlug : title;
        String currentSlug = current == null ? null : current.getSlug();
        return SlugGenerator.toUniqueSlug(source, title,
                candidate -> !candidate.equals(currentSlug) && jobRepo.existsBySlug(candidate));
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
