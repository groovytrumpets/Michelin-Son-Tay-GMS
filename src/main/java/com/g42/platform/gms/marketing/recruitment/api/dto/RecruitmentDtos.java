package com.g42.platform.gms.marketing.recruitment.api.dto;

import com.g42.platform.gms.marketing.recruitment.domain.ApplicationStatus;
import com.g42.platform.gms.marketing.recruitment.domain.EmploymentType;
import com.g42.platform.gms.marketing.recruitment.domain.JobStatus;
import com.g42.platform.gms.marketing.recruitment.domain.NotifyStatus;
import com.g42.platform.gms.marketing.recruitment.domain.SalaryPeriod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Toàn bộ kiểu dữ liệu vào/ra của phân hệ tuyển dụng, gom một chỗ cho dễ đối
 * chiếu với recruitmentService.js phía FE.
 */
public final class RecruitmentDtos {

    private RecruitmentDtos() {
    }

    // ------------------------------------------------------------ mức hỗ trợ

    /** Một khoản hỗ trợ hiển thị thành chip trên thẻ vị trí. */
    public record BenefitDto(
            Long benefitId,
            @NotBlank(message = "Tên khoản hỗ trợ không được để trống")
            @Size(max = 150) String label,
            @Size(max = 250) String valueText,
            @Size(max = 40) String icon,
            Integer displayOrder
    ) {
    }

    // -------------------------------------------------- câu hỏi thêm của form

    /**
     * Một câu hỏi thêm do người đăng tin tự định nghĩa.
     *
     * <p>Không có bảng riêng: cả danh sách được lưu thành một chuỗi JSON trong
     * {@code job_position.form_fields_json}, nên thêm loại câu hỏi mới chỉ là
     * việc của FE, không phải chạy migration.
     */
    public record FormFieldDto(
            /** Khoá dùng làm key trong answers_json; hệ thống tự sinh nếu bỏ trống. */
            @Size(max = 60) String key,
            @NotBlank(message = "Câu hỏi không được để trống")
            @Size(max = 200) String label,
            /** TEXT / TEXTAREA / NUMBER / SELECT / CHECKBOX / DATE */
            @Size(max = 20) String type,
            Boolean required,
            @Size(max = 200) String placeholder,
            List<String> options
    ) {
    }

    // ---------------------------------------------------------- tin tuyển dụng

    /** Bản rút gọn cho danh sách vị trí (cả trang khách lẫn màn hình quản trị). */
    public record JobSummaryDto(
            Long jobId,
            String slug,
            String title,
            String department,
            EmploymentType employmentType,
            String workLocation,
            Integer headcount,
            String experienceText,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            SalaryPeriod salaryPeriod,
            String salaryText,
            String summary,
            String thumbnailUrl,
            JobStatus status,
            Boolean isFeatured,
            Boolean isUrgent,
            Integer displayOrder,
            LocalDateTime publishedAt,
            LocalDate closingDate,
            Long viewCount,
            Integer applicationCount,
            /** Tính sẵn ở backend để FE không phải tự so hạn nộp với ngày hiện tại. */
            Boolean acceptingApplications,
            List<BenefitDto> benefits
    ) {
    }

    /** Bản đầy đủ cho trang chi tiết công khai. */
    public record JobDetailDto(
            Long jobId,
            String slug,
            String title,
            String department,
            EmploymentType employmentType,
            String workLocation,
            Integer headcount,
            String experienceText,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            SalaryPeriod salaryPeriod,
            String salaryText,
            String summary,
            String contentHtml,
            String requirementHtml,
            String benefitHtml,
            String thumbnailUrl,
            String coverUrl,
            JobStatus status,
            Boolean isUrgent,
            LocalDateTime publishedAt,
            LocalDate closingDate,
            Long viewCount,
            Boolean acceptingApplications,
            String seoTitle,
            String seoDescription,
            String ogImageUrl,
            Boolean allowIndex,
            List<BenefitDto> benefits,
            List<FormFieldDto> formFields
    ) {
    }

    /** Bản quản trị: thêm thông tin duyệt tin và hộp thư nhận riêng. */
    public record JobAdminDetailDto(
            Long jobId,
            String slug,
            String title,
            String department,
            EmploymentType employmentType,
            String workLocation,
            Integer headcount,
            String experienceText,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            SalaryPeriod salaryPeriod,
            String salaryText,
            String summary,
            String contentHtml,
            String requirementHtml,
            String benefitHtml,
            String thumbnailUrl,
            String coverUrl,
            JobStatus status,
            Boolean isFeatured,
            Boolean isUrgent,
            Integer displayOrder,
            LocalDateTime publishedAt,
            LocalDate closingDate,
            Integer authorStaffId,
            String authorName,
            Integer reviewerStaffId,
            String reviewerName,
            String reviewNote,
            Long viewCount,
            Integer applicationCount,
            String notifyEmails,
            String seoTitle,
            String seoDescription,
            String ogImageUrl,
            Boolean allowIndex,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<BenefitDto> benefits,
            List<FormFieldDto> formFields
    ) {
    }

    /** Dữ liệu tạo/sửa tin tuyển dụng. */
    public record JobSaveRequest(
            @NotBlank(message = "Tên vị trí không được để trống")
            @Size(max = 250, message = "Tên vị trí tối đa 250 ký tự")
            String title,
            /** Bỏ trống thì hệ thống tự sinh từ tên vị trí. */
            @Size(max = 200) String slug,
            @Size(max = 120) String department,
            EmploymentType employmentType,
            @Size(max = 250) String workLocation,
            Integer headcount,
            @Size(max = 150) String experienceText,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            SalaryPeriod salaryPeriod,
            @Size(max = 150) String salaryText,
            @Size(max = 500) String summary,
            String contentHtml,
            String requirementHtml,
            String benefitHtml,
            @Size(max = 500) String thumbnailUrl,
            @Size(max = 500) String coverUrl,
            Boolean isFeatured,
            Boolean isUrgent,
            Integer displayOrder,
            LocalDate closingDate,
            /** Hộp thư nhận riêng cho vị trí; trống thì dùng cấu hình chung. */
            @Size(max = 500) String notifyEmails,
            @Size(max = 200) String seoTitle,
            @Size(max = 320) String seoDescription,
            @Size(max = 500) String ogImageUrl,
            Boolean allowIndex,
            List<BenefitDto> benefits,
            List<FormFieldDto> formFields
    ) {
    }

    /** Đổi trạng thái tin tuyển dụng. */
    public record JobStatusChangeRequest(
            JobStatus status,
            /** Lý do trả tin về nháp, hiện cho người soạn. */
            @Size(max = 500) String reviewNote
    ) {
    }

    // ------------------------------------------------------------- hồ sơ ứng tuyển

    /**
     * Hồ sơ ứng viên gửi lên từ trang khách.
     *
     * <p>{@code website} là bẫy bot (honeypot): ô ẩn mà người thật không bao giờ
     * nhìn thấy nên không bao giờ điền. Có giá trị nghĩa là bot điền hộ, backend
     * lặng lẽ bỏ qua thay vì báo lỗi để bot không dò ra được luật.
     */
    public record ApplicationSubmitRequest(
            @NotBlank(message = "Họ tên không được để trống")
            @Size(max = 150) String fullName,
            @NotBlank(message = "Số điện thoại không được để trống")
            @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại phải gồm 10-11 chữ số và bắt đầu bằng 0")
            String phone,
            @Email(message = "Email không hợp lệ")
            @Size(max = 180) String email,
            LocalDate dateOfBirth,
            @Size(max = 10) String gender,
            @Size(max = 300) String address,
            Integer yearsExperience,
            @Size(max = 150) String currentPosition,
            BigDecimal expectedSalary,
            LocalDate availableFrom,
            @Size(max = 3000, message = "Thư giới thiệu tối đa 3000 ký tự")
            String coverLetter,
            @Size(max = 500) String cvUrl,
            @Size(max = 500) String portfolioUrl,
            Map<String, String> answers,
            @Size(max = 120) String utmSource,
            @Size(max = 120) String utmCampaign,
            String website
    ) {
    }

    /** Phản hồi ngay sau khi gửi hồ sơ — ứng viên cần thấy mã để đối chiếu sau này. */
    public record ApplicationReceiptDto(
            String code,
            String jobTitle,
            LocalDateTime submittedAt,
            String message
    ) {
    }

    /** Bản rút gọn cho bảng hồ sơ trong khu quản trị. */
    public record ApplicationSummaryDto(
            Long applicationId,
            String code,
            Long jobId,
            String jobTitle,
            String fullName,
            String phone,
            String email,
            Integer yearsExperience,
            String currentPosition,
            ApplicationStatus status,
            Integer rating,
            NotifyStatus notifyStatus,
            String handledByName,
            Boolean hasCv,
            LocalDateTime createdAt
    ) {
    }

    /** Bản đầy đủ khi mở một hồ sơ. */
    public record ApplicationDetailDto(
            Long applicationId,
            String code,
            Long jobId,
            String jobTitle,
            String jobSlug,
            String fullName,
            String phone,
            String email,
            LocalDate dateOfBirth,
            String gender,
            String address,
            Integer yearsExperience,
            String currentPosition,
            BigDecimal expectedSalary,
            LocalDate availableFrom,
            String coverLetter,
            String cvUrl,
            String portfolioUrl,
            Map<String, String> answers,
            /** Nhãn câu hỏi tại thời điểm xem, để hiện "Câu hỏi: trả lời" cho dễ đọc. */
            List<FormFieldDto> formFields,
            ApplicationStatus status,
            Integer rating,
            String internalNote,
            String handledByName,
            LocalDateTime handledAt,
            NotifyStatus notifyStatus,
            String notifyError,
            LocalDateTime notifiedAt,
            Boolean ackSent,
            String utmSource,
            String utmCampaign,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /** Cập nhật trạng thái xử lý / đánh giá / ghi chú của một hồ sơ. */
    public record ApplicationUpdateRequest(
            ApplicationStatus status,
            Integer rating,
            String internalNote
    ) {
    }

    // ----------------------------------------------------------------- cấu hình

    /** Cấu hình trang tuyển dụng + hộp thư nhận hồ sơ (bản quản trị). */
    public record SettingDto(
            Boolean isPageEnabled,
            @Size(max = 200) String pageTitle,
            @Size(max = 500) String pageSubtitle,
            String pageIntroHtml,
            @Size(max = 500) String bannerUrl,
            @Size(max = 30) String contactHotline,
            @Size(max = 300) String contactAddress,
            Boolean notifyEnabled,
            @Size(max = 1000) String recipientEmails,
            @Size(max = 1000) String ccEmails,
            @Size(max = 100) String subjectPrefix,
            Boolean sendAckEnabled,
            @Size(max = 200) String ackSubject,
            String ackBodyHtml,
            @Size(max = 200) String seoTitle,
            @Size(max = 320) String seoDescription,
            @Size(max = 500) String ogImageUrl,
            LocalDateTime updatedAt,
            String updatedByName,
            /**
             * Máy chủ SMTP đã cấu hình xong hay chưa. Không có nó thì người dùng
             * điền đủ email người nhận mà thư vẫn không bao giờ tới, và trên màn
             * hình chẳng có gì cho thấy vì sao.
             */
            Boolean mailConfigured
    ) {
    }

    /** Bản công khai của cấu hình — chỉ phần trang khách được phép biết. */
    public record PublicPageDto(
            Boolean isPageEnabled,
            String pageTitle,
            String pageSubtitle,
            String pageIntroHtml,
            String bannerUrl,
            String contactHotline,
            String contactAddress,
            String seoTitle,
            String seoDescription,
            String ogImageUrl,
            List<String> departments
    ) {
    }

    /** Gửi thử một thư tới danh sách người nhận đang cấu hình. */
    public record TestMailRequest(
            @Email(message = "Email không hợp lệ")
            @Size(max = 180) String to
    ) {
    }

    /** Thống kê cho màn hình quản trị. */
    public record StatsDto(
            long draft,
            long pending,
            long published,
            long closed,
            long newApplications,
            long applicationsThisMonth,
            long totalApplications
    ) {
    }
}
