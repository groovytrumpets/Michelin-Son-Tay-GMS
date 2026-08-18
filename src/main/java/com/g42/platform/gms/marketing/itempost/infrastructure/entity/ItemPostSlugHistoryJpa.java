package com.g42.platform.gms.marketing.itempost.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Slug cũ của bài viết phụ tùng. Khi biên tập đổi đường dẫn, link đã chia sẻ
 * trên Facebook/Zalo vẫn còn sống: API trả về slug mới để trình duyệt chuyển
 * hướng, nhờ đó không mất thứ hạng đã tích luỹ trên Google.
 */
@Getter
@Setter
@Entity
@Table(name = "item_post_slug_history")
public class ItemPostSlugHistoryJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", nullable = false)
    private Long historyId;

    @Column(name = "item_post_id", nullable = false)
    private Long itemPostId;

    @Column(name = "old_slug", nullable = false, length = 200, unique = true)
    private String oldSlug;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
