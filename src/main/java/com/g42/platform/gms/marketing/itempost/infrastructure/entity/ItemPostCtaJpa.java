package com.g42.platform.gms.marketing.itempost.infrastructure.entity;

import com.g42.platform.gms.marketing.itempost.domain.ItemPostCtaPosition;
import com.g42.platform.gms.marketing.itempost.domain.ItemPostCtaType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Khối kêu gọi hành động chèn trong bài viết phụ tùng — biến lượt đọc thành lượt đặt lịch. */
@Getter
@Setter
@Entity
@Table(name = "item_post_cta")
public class ItemPostCtaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cta_id", nullable = false)
    private Long ctaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_post_id", nullable = false)
    private ItemPostJpa itemPost;

    @Enumerated(EnumType.STRING)
    @Column(name = "cta_type", nullable = false, length = 20)
    private ItemPostCtaType ctaType;

    @Column(name = "label", length = 150)
    private String label;

    @Column(name = "sub_label", length = 250)
    private String subLabel;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    /** Trỏ thẳng sang mặt hàng để trang bài lấy được tên/ảnh/giá đang bán. */
    @Column(name = "catalog_item_id")
    private Integer catalogItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "position", length = 20)
    private ItemPostCtaPosition position;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive;
}
