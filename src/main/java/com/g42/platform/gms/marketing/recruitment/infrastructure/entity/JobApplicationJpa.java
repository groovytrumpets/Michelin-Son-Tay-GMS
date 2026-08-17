package com.g42.platform.gms.marketing.recruitment.infrastructure.entity;

import com.g42.platform.gms.marketing.recruitment.domain.ApplicationStatus;
import com.g42.platform.gms.marketing.recruitment.domain.NotifyStatus;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Một lượt ứng viên nộp hồ sơ qua form trên trang khách. */
@Getter
@Setter
@Entity
@Table(name = "job_application")
public class JobApplicationJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    /** Mã đọc được (UT2608-0007), dùng khi trao đổi với ứng viên qua điện thoại. */
    @Column(name = "code", nullable = false, length = 30, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPositionJpa job;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "address", length = 300)
    private String address;

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "current_position", length = 150)
    private String currentPosition;

    @Column(name = "expected_salary", precision = 14, scale = 2)
    private BigDecimal expectedSalary;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Column(name = "cv_url", length = 500)
    private String cvUrl;

    @Column(name = "portfolio_url", length = 500)
    private String portfolioUrl;

    /** JSON {key: giá trị} cho các câu hỏi thêm mà tin tuyển dụng tự định nghĩa. */
    @Column(name = "answers_json", columnDefinition = "TEXT")
    private String answersJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "rating")
    private Integer rating;

    /** Ghi chú nội bộ — không bao giờ trả ra API công khai. */
    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by_staff_id")
    private StaffProfileJpa handledBy;

    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "notify_status", length = 20)
    private NotifyStatus notifyStatus;

    @Column(name = "notify_error", length = 500)
    private String notifyError;

    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    @Column(name = "ack_sent")
    private Boolean ackSent;

    @Column(name = "utm_source", length = 120)
    private String utmSource;

    @Column(name = "utm_campaign", length = 120)
    private String utmCampaign;

    /** SHA-256 của IP + muối; dùng chặn gửi trùng liên tục, không lưu IP thô. */
    @Column(name = "submitter_hash", length = 64)
    private String submitterHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
