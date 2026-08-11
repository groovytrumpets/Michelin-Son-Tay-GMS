package com.g42.platform.gms.marketing.news.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Danh mục tin tức — một bài viết thuộc tối đa một danh mục. */
@Getter
@Setter
@Entity
@Table(name = "post_category")
public class PostCategoryJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    /** Đoạn định danh dùng trong URL /tin-tuc/danh-muc/{slug}. */
    @Column(name = "slug", nullable = false, length = 160, unique = true)
    private String slug;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 320)
    private String seoDescription;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
