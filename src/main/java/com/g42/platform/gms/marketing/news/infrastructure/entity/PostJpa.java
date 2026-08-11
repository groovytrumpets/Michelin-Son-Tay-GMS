package com.g42.platform.gms.marketing.news.infrastructure.entity;

import com.g42.platform.gms.marketing.news.domain.PostStatus;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Bài viết tin tức. Khác với bảng {@code service} (blog gắn dưới một mặt hàng),
 * đây là thực thể độc lập có đường dẫn riêng để chia sẻ và cho Google index.
 */
@Getter
@Setter
@Entity
@Table(name = "post")
public class PostJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", nullable = false)
    private Long postId;

    /** Khoá của URL /tin-tuc/{slug}. Không nhét id vào để đường dẫn sạch. */
    @Column(name = "slug", nullable = false, length = 200, unique = true)
    private String slug;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "excerpt", length = 500)
    private String excerpt;

    @Column(name = "content_html", columnDefinition = "LONGTEXT")
    private String contentHtml;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private PostCategoryJpa category;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PostStatus status;

    @Column(name = "is_featured")
    private Boolean isFeatured;

    /** Có giá trị nghĩa là bài được ghim lên đầu, sắp xếp tăng dần. */
    @Column(name = "pinned_order")
    private Integer pinnedOrder;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

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

    @Column(name = "share_count")
    private Long shareCount;

    @Column(name = "reading_minutes")
    private Integer readingMinutes;

    @Column(name = "seo_title", length = 200)
    private String seoTitle;

    @Column(name = "seo_description", length = 320)
    private String seoDescription;

    @Column(name = "seo_keywords", length = 500)
    private String seoKeywords;

    @Column(name = "og_image_url", length = 500)
    private String ogImageUrl;

    @Column(name = "canonical_url", length = 500)
    private String canonicalUrl;

    /** Tắt để gắn thẻ noindex — dùng cho bài nội bộ không muốn lên Google. */
    @Column(name = "allow_index")
    private Boolean allowIndex;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Xoá mềm: bài đã xoá vẫn còn bản ghi để không chết link đã chia sẻ. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "post_tag_map",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<PostTagJpa> tags = new LinkedHashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("displayOrder ASC")
    private List<PostCtaJpa> ctas = new ArrayList<>();

    /** Bài có đang thực sự hiển thị cho khách hay không. */
    public boolean isVisibleToPublic() {
        return deletedAt == null
                && status == PostStatus.PUBLISHED
                && publishedAt != null
                && !publishedAt.isAfter(LocalDateTime.now());
    }
}
