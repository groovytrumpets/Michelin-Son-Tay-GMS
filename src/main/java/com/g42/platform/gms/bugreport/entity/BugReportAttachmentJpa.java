package com.g42.platform.gms.bugreport.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Ảnh chụp màn hình đính kèm phiếu báo lỗi (URL Cloudinary). */
@Getter
@Setter
@Entity
@Table(name = "bug_report_attachment")
public class BugReportAttachmentJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id", nullable = false)
    private Long attachmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private BugReportJpa bugReport;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;
}
