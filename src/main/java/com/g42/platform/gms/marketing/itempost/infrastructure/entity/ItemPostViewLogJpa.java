package com.g42.platform.gms.marketing.itempost.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Một lượt xem đã qua khử trùng lặp. Ngoài việc đếm view, bảng này trả lời câu
 * hỏi "kênh nào mang người đọc về" nhờ bộ tham số UTM đính trong link chia sẻ.
 */
@Getter
@Setter
@Entity
@Table(name = "item_post_view_log")
public class ItemPostViewLogJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id", nullable = false)
    private Long viewId;

    @Column(name = "item_post_id", nullable = false)
    private Long itemPostId;

    /** SHA-256 của IP + User-Agent + muối. Không lưu IP thô. */
    @Column(name = "visitor_hash", length = 64)
    private String visitorHash;

    @Column(name = "session_key", length = 64)
    private String sessionKey;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "referrer", length = 500)
    private String referrer;

    @Column(name = "utm_source", length = 120)
    private String utmSource;

    @Column(name = "utm_medium", length = 120)
    private String utmMedium;

    @Column(name = "utm_campaign", length = 120)
    private String utmCampaign;

    @Column(name = "utm_content", length = 120)
    private String utmContent;

    @Column(name = "device", length = 20)
    private String device;
}
