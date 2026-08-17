package com.g42.platform.gms.marketing.recruitment.infrastructure.entity;

import com.g42.platform.gms.marketing.recruitment.domain.EmploymentType;
import com.g42.platform.gms.marketing.recruitment.domain.JobStatus;
import com.g42.platform.gms.marketing.recruitment.domain.SalaryPeriod;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một tin tuyển dụng: vừa mang thông tin vị trí (bộ phận, số lượng, mức lương)
 * vừa mang nội dung bài viết (slug, HTML mô tả, SEO).
 */
@Getter
@Setter
@Entity
@Table(name = "job_position")
public class JobPositionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_id", nullable = false)
    private Long jobId;

    /** Khoá của URL /tuyen-dung/{slug}. */
    @Column(name = "slug", nullable = false, length = 200, unique = true)
    private String slug;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "department", length = 120)
    private String department;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", length = 20)
    private EmploymentType employmentType;

    @Column(name = "work_location", length = 250)
    private String workLocation;

    @Column(name = "headcount")
    private Integer headcount;

    @Column(name = "experience_text", length = 150)
    private String experienceText;

    @Column(name = "salary_min", precision = 14, scale = 2)
    private BigDecimal salaryMin;

    @Column(name = "salary_max", precision = 14, scale = 2)
    private BigDecimal salaryMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_period", length = 20)
    private SalaryPeriod salaryPeriod;

    /** Ghi đè khoảng lương khi muốn hiện "Thoả thuận" thay vì con số. */
    @Column(name = "salary_text", length = 150)
    private String salaryText;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "content_html", columnDefinition = "LONGTEXT")
    private String contentHtml;

    @Column(name = "requirement_html", columnDefinition = "LONGTEXT")
    private String requirementHtml;

    @Column(name = "benefit_html", columnDefinition = "LONGTEXT")
    private String benefitHtml;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "is_featured")
    private Boolean isFeatured;

    @Column(name = "is_urgent")
    private Boolean isUrgent;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /** Hạn nhận hồ sơ; quá ngày này thì form đăng ký tự khoá. */
    @Column(name = "closing_date")
    private LocalDate closingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_staff_id")
    private StaffProfileJpa author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_staff_id")
    private StaffProfileJpa reviewer;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "view_count")
    private Long viewCount;

    @Column(name = "application_count")
    private Integer applicationCount;

    /** Hộp thư nhận hồ sơ riêng cho vị trí này; trống thì dùng cấu hình chung. */
    @Column(name = "notify_emails", length = 500)
    private String notifyEmails;

    /** JSON các câu hỏi thêm của form đăng ký — xem RecruitmentFormFields. */
    @Column(name = "form_fields_json", columnDefinition = "LONGTEXT")
    private String formFieldsJson;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 320)
    private String seoDescription;

    @Column(name = "og_image_url", length = 500)
    private String ogImageUrl;

    @Column(name = "allow_index")
    private Boolean allowIndex;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Xoá mềm — giữ bản ghi để hồ sơ đã nộp không mất vị trí gốc. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<JobBenefitJpa> benefits = new ArrayList<>();

    /** Tin còn hiện ngoài trang danh sách của khách. */
    public boolean isVisibleToPublic() {
        return deletedAt == null && status != null && status.isListedToPublic();
    }

    /**
     * Còn nhận hồ sơ hay không. Hạn nộp được xét ở đây thay vì dựa vào một job
     * nền đổi trạng thái: quá nửa đêm là form khoá ngay, không phụ thuộc lịch
     * chạy nền và cũng không cần ghi lại vào DB.
     */
    public boolean isOpenForApplication() {
        if (deletedAt != null || status == null || !status.acceptsApplications()) return false;
        return closingDate == null || !closingDate.isBefore(LocalDate.now());
    }
}
