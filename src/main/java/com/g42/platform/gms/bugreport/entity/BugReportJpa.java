package com.g42.platform.gms.bugreport.entity;

import com.g42.platform.gms.bugreport.enums.BugReportCategory;
import com.g42.platform.gms.bugreport.enums.BugReportSeverity;
import com.g42.platform.gms.bugreport.enums.BugReportStatus;
import com.g42.platform.gms.bugreport.enums.ReporterType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "bug_report")
public class BugReportJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private BugReportCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private BugReportSeverity severity;

    /** Phân hệ nghi ngờ phát sinh lỗi — dùng chung bộ mã với system_log (BOOKING, WAREHOUSE...). */
    @Column(name = "module", length = 40)
    private String module;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BugReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reporter_type", nullable = false, length = 20)
    private ReporterType reporterType;

    @Column(name = "reporter_staff_id")
    private Integer reporterStaffId;

    @Column(name = "reporter_customer_id")
    private Integer reporterCustomerId;

    @Column(name = "reporter_name", length = 150)
    private String reporterName;

    @Column(name = "reporter_role", length = 50)
    private String reporterRole;

    @Column(name = "reporter_contact", length = 100)
    private String reporterContact;

    /** Ngữ cảnh kỹ thuật do trình duyệt tự thu thập, giúp lập trình viên tái hiện lỗi. */
    @Column(name = "page_url", length = 500)
    private String pageUrl;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "screen_size", length = 40)
    private String screenSize;

    @Column(name = "app_version", length = 40)
    private String appVersion;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "assigned_staff_id")
    private Integer assignedStaffId;

    @Column(name = "assigned_staff_name", length = 150)
    private String assignedStaffName;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "bugReport", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BugReportAttachmentJpa> attachments = new ArrayList<>();

    public void addAttachment(String imageUrl) {
        BugReportAttachmentJpa attachment = new BugReportAttachmentJpa();
        attachment.setImageUrl(imageUrl);
        attachment.setBugReport(this);
        attachments.add(attachment);
    }
}
