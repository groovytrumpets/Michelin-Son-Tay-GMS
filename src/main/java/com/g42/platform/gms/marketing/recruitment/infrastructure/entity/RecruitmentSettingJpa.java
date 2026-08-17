package com.g42.platform.gms.marketing.recruitment.infrastructure.entity;

import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Cấu hình dùng chung của phân hệ tuyển dụng — bảng một dòng (setting_id = 1).
 *
 * <p>Danh sách email nhận hồ sơ nằm ở đây chứ không ở biến môi trường: quản lý
 * phải tự đổi được người nhận từ trong khu quản trị, không phải sửa file cấu
 * hình rồi khởi động lại ứng dụng.
 */
@Getter
@Setter
@Entity
@Table(name = "recruitment_setting")
public class RecruitmentSettingJpa {

    /** Luôn là 1 — xem RecruitmentSettingService để biết vì sao chỉ có một dòng. */
    public static final int SINGLETON_ID = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id", nullable = false)
    private Integer settingId;

    @Column(name = "is_page_enabled")
    private Boolean isPageEnabled;

    @Column(name = "page_title", length = 200)
    private String pageTitle;

    @Column(name = "page_subtitle", length = 500)
    private String pageSubtitle;

    @Column(name = "page_intro_html", columnDefinition = "LONGTEXT")
    private String pageIntroHtml;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "contact_hotline", length = 30)
    private String contactHotline;

    @Column(name = "contact_address", length = 300)
    private String contactAddress;

    @Column(name = "notify_enabled")
    private Boolean notifyEnabled;

    /** Danh sách email nhận hồ sơ, phân tách bằng dấu phẩy. */
    @Column(name = "recipient_emails", length = 1000)
    private String recipientEmails;

    @Column(name = "cc_emails", length = 1000)
    private String ccEmails;

    /** Tiền tố tiêu đề thư, giúp quản lý đặt bộ lọc trong hộp thư. */
    @Column(name = "subject_prefix", length = 100)
    private String subjectPrefix;

    @Column(name = "send_ack_enabled")
    private Boolean sendAckEnabled;

    @Column(name = "ack_subject", length = 200)
    private String ackSubject;

    /** Thân thư cảm ơn; hỗ trợ biến {{ten}}, {{vitri}}, {{mahoso}}. */
    @Column(name = "ack_body_html", columnDefinition = "LONGTEXT")
    private String ackBodyHtml;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 320)
    private String seoDescription;

    @Column(name = "og_image_url", length = 500)
    private String ogImageUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_staff_id")
    private StaffProfileJpa updatedBy;
}
