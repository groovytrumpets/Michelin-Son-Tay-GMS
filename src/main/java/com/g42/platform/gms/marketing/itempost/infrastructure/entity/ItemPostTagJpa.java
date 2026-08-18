package com.g42.platform.gms.marketing.itempost.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Tag tự do gắn cho bài viết phụ tùng, dùng cho trang /phu-tung/tag/{slug} và gợi ý bài liên quan. */
@Getter
@Setter
@Entity
@Table(name = "item_post_tag")
public class ItemPostTagJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id", nullable = false)
    private Integer tagId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, length = 120, unique = true)
    private String slug;

    /** Số bài đang dùng tag này — để xếp hạng tag phổ biến mà không phải đếm lại. */
    @Column(name = "usage_count")
    private Integer usageCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
