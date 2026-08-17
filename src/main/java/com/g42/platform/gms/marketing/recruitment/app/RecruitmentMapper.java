package com.g42.platform.gms.marketing.recruitment.app;

import com.g42.platform.gms.marketing.recruitment.api.dto.RecruitmentDtos;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobApplicationJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobBenefitJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobPositionJpa;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.RecruitmentSettingJpa;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Chuyển entity sang DTO. Nơi duy nhất quyết định trường nào được lộ ra ngoài. */
@Component
@RequiredArgsConstructor
public class RecruitmentMapper {

    private final RecruitmentJsonCodec jsonCodec;

    // ------------------------------------------------------------ mức hỗ trợ

    public RecruitmentDtos.BenefitDto toBenefitDto(JobBenefitJpa benefit) {
        return new RecruitmentDtos.BenefitDto(
                benefit.getBenefitId(),
                benefit.getLabel(),
                benefit.getValueText(),
                benefit.getIcon(),
                benefit.getDisplayOrder());
    }

    private List<RecruitmentDtos.BenefitDto> toBenefitDtos(JobPositionJpa job) {
        return job.getBenefits().stream().map(this::toBenefitDto).toList();
    }

    // ------------------------------------------------------- tin tuyển dụng

    public RecruitmentDtos.JobSummaryDto toSummary(JobPositionJpa job) {
        return new RecruitmentDtos.JobSummaryDto(
                job.getJobId(),
                job.getSlug(),
                job.getTitle(),
                job.getDepartment(),
                job.getEmploymentType(),
                job.getWorkLocation(),
                job.getHeadcount(),
                job.getExperienceText(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryPeriod(),
                job.getSalaryText(),
                job.getSummary(),
                job.getThumbnailUrl(),
                job.getStatus(),
                job.getIsFeatured(),
                job.getIsUrgent(),
                job.getDisplayOrder(),
                job.getPublishedAt(),
                job.getClosingDate(),
                job.getViewCount(),
                job.getApplicationCount(),
                job.isOpenForApplication(),
                toBenefitDtos(job));
    }

    public RecruitmentDtos.JobDetailDto toPublicDetail(JobPositionJpa job) {
        return new RecruitmentDtos.JobDetailDto(
                job.getJobId(),
                job.getSlug(),
                job.getTitle(),
                job.getDepartment(),
                job.getEmploymentType(),
                job.getWorkLocation(),
                job.getHeadcount(),
                job.getExperienceText(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryPeriod(),
                job.getSalaryText(),
                job.getSummary(),
                job.getContentHtml(),
                job.getRequirementHtml(),
                job.getBenefitHtml(),
                job.getThumbnailUrl(),
                job.getCoverUrl(),
                job.getStatus(),
                job.getIsUrgent(),
                job.getPublishedAt(),
                job.getClosingDate(),
                job.getViewCount(),
                job.isOpenForApplication(),
                job.getSeoTitle(),
                job.getSeoDescription(),
                job.getOgImageUrl(),
                job.getAllowIndex(),
                toBenefitDtos(job),
                jsonCodec.readFields(job.getFormFieldsJson()));
    }

    public RecruitmentDtos.JobAdminDetailDto toAdminDetail(JobPositionJpa job) {
        StaffProfileJpa author = job.getAuthor();
        StaffProfileJpa reviewer = job.getReviewer();
        return new RecruitmentDtos.JobAdminDetailDto(
                job.getJobId(),
                job.getSlug(),
                job.getTitle(),
                job.getDepartment(),
                job.getEmploymentType(),
                job.getWorkLocation(),
                job.getHeadcount(),
                job.getExperienceText(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getSalaryPeriod(),
                job.getSalaryText(),
                job.getSummary(),
                job.getContentHtml(),
                job.getRequirementHtml(),
                job.getBenefitHtml(),
                job.getThumbnailUrl(),
                job.getCoverUrl(),
                job.getStatus(),
                job.getIsFeatured(),
                job.getIsUrgent(),
                job.getDisplayOrder(),
                job.getPublishedAt(),
                job.getClosingDate(),
                author == null ? null : author.getStaffId(),
                staffName(author),
                reviewer == null ? null : reviewer.getStaffId(),
                staffName(reviewer),
                job.getReviewNote(),
                job.getViewCount(),
                job.getApplicationCount(),
                job.getNotifyEmails(),
                job.getSeoTitle(),
                job.getSeoDescription(),
                job.getOgImageUrl(),
                job.getAllowIndex(),
                job.getCreatedAt(),
                job.getUpdatedAt(),
                toBenefitDtos(job),
                jsonCodec.readFields(job.getFormFieldsJson()));
    }

    // ------------------------------------------------------ hồ sơ ứng tuyển

    public RecruitmentDtos.ApplicationSummaryDto toApplicationSummary(JobApplicationJpa application) {
        JobPositionJpa job = application.getJob();
        return new RecruitmentDtos.ApplicationSummaryDto(
                application.getApplicationId(),
                application.getCode(),
                job == null ? null : job.getJobId(),
                job == null ? null : job.getTitle(),
                application.getFullName(),
                application.getPhone(),
                application.getEmail(),
                application.getYearsExperience(),
                application.getCurrentPosition(),
                application.getStatus(),
                application.getRating(),
                application.getNotifyStatus(),
                staffName(application.getHandledBy()),
                application.getCvUrl() != null && !application.getCvUrl().isBlank(),
                application.getCreatedAt());
    }

    public RecruitmentDtos.ApplicationDetailDto toApplicationDetail(JobApplicationJpa application) {
        JobPositionJpa job = application.getJob();
        return new RecruitmentDtos.ApplicationDetailDto(
                application.getApplicationId(),
                application.getCode(),
                job == null ? null : job.getJobId(),
                job == null ? null : job.getTitle(),
                job == null ? null : job.getSlug(),
                application.getFullName(),
                application.getPhone(),
                application.getEmail(),
                application.getDateOfBirth(),
                application.getGender(),
                application.getAddress(),
                application.getYearsExperience(),
                application.getCurrentPosition(),
                application.getExpectedSalary(),
                application.getAvailableFrom(),
                application.getCoverLetter(),
                application.getCvUrl(),
                application.getPortfolioUrl(),
                jsonCodec.readAnswers(application.getAnswersJson()),
                job == null ? List.of() : jsonCodec.readFields(job.getFormFieldsJson()),
                application.getStatus(),
                application.getRating(),
                application.getInternalNote(),
                staffName(application.getHandledBy()),
                application.getHandledAt(),
                application.getNotifyStatus(),
                application.getNotifyError(),
                application.getNotifiedAt(),
                application.getAckSent(),
                application.getUtmSource(),
                application.getUtmCampaign(),
                application.getCreatedAt(),
                application.getUpdatedAt());
    }

    // ----------------------------------------------------------------- cấu hình

    public RecruitmentDtos.SettingDto toSettingDto(RecruitmentSettingJpa setting, boolean mailConfigured) {
        return new RecruitmentDtos.SettingDto(
                setting.getIsPageEnabled(),
                setting.getPageTitle(),
                setting.getPageSubtitle(),
                setting.getPageIntroHtml(),
                setting.getBannerUrl(),
                setting.getContactHotline(),
                setting.getContactAddress(),
                setting.getNotifyEnabled(),
                setting.getRecipientEmails(),
                setting.getCcEmails(),
                setting.getSubjectPrefix(),
                setting.getSendAckEnabled(),
                setting.getAckSubject(),
                setting.getAckBodyHtml(),
                setting.getSeoTitle(),
                setting.getSeoDescription(),
                setting.getOgImageUrl(),
                setting.getUpdatedAt(),
                staffName(setting.getUpdatedBy()),
                mailConfigured);
    }

    public RecruitmentDtos.PublicPageDto toPublicPageDto(RecruitmentSettingJpa setting, List<String> departments) {
        return new RecruitmentDtos.PublicPageDto(
                setting.getIsPageEnabled(),
                setting.getPageTitle(),
                setting.getPageSubtitle(),
                setting.getPageIntroHtml(),
                setting.getBannerUrl(),
                setting.getContactHotline(),
                setting.getContactAddress(),
                setting.getSeoTitle(),
                setting.getSeoDescription(),
                setting.getOgImageUrl(),
                departments);
    }

    private String staffName(StaffProfileJpa staff) {
        return staff == null ? null : staff.getFullName();
    }
}
