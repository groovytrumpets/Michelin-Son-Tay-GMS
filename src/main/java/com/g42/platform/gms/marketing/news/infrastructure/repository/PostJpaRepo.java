package com.g42.platform.gms.marketing.news.infrastructure.repository;

import com.g42.platform.gms.marketing.news.domain.PostStatus;
import com.g42.platform.gms.marketing.news.infrastructure.entity.PostJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostJpaRepo extends JpaRepository<PostJpa, Long>, JpaSpecificationExecutor<PostJpa> {

    Optional<PostJpa> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlug(String slug);

    Optional<PostJpa> findByPostIdAndDeletedAtIsNull(Long postId);

    /** Bài tới hạn đăng — job nền gọi mỗi phút. */
    List<PostJpa> findByStatusAndScheduledAtLessThanEqualAndDeletedAtIsNull(PostStatus status, LocalDateTime moment);

    /**
     * Bài liên quan: cùng danh mục hoặc trùng ít nhất một tag, xếp theo số tag
     * trùng giảm dần rồi tới ngày đăng. Giữ người đọc ở lại thêm một bài nữa.
     */
    @Query("""
            SELECT p FROM PostJpa p
            LEFT JOIN p.tags t
            WHERE p.postId <> :postId
              AND p.deletedAt IS NULL
              AND p.status = com.g42.platform.gms.marketing.news.domain.PostStatus.PUBLISHED
              AND p.publishedAt <= :now
              AND (:categoryId IS NULL AND :hasTags = FALSE
                   OR p.category.categoryId = :categoryId
                   OR t.tagId IN :tagIds)
            GROUP BY p
            ORDER BY COUNT(t.tagId) DESC, p.publishedAt DESC
            """)
    List<PostJpa> findRelated(@Param("postId") Long postId,
                              @Param("categoryId") Integer categoryId,
                              @Param("tagIds") List<Integer> tagIds,
                              @Param("hasTags") boolean hasTags,
                              @Param("now") LocalDateTime now,
                              Pageable pageable);

    /** Nguồn cho sitemap và RSS — chỉ bài công khai và cho phép index. */
    @Query("""
            SELECT p FROM PostJpa p
            WHERE p.deletedAt IS NULL
              AND p.status = com.g42.platform.gms.marketing.news.domain.PostStatus.PUBLISHED
              AND p.publishedAt <= :now
              AND (p.allowIndex IS NULL OR p.allowIndex = TRUE)
            ORDER BY p.publishedAt DESC
            """)
    List<PostJpa> findIndexablePublished(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            SELECT p FROM PostJpa p
            WHERE p.deletedAt IS NULL
              AND p.status = com.g42.platform.gms.marketing.news.domain.PostStatus.PUBLISHED
              AND p.publishedAt BETWEEN :from AND :now
              AND (p.allowIndex IS NULL OR p.allowIndex = TRUE)
            ORDER BY p.publishedAt DESC
            """)
    List<PostJpa> findRecentForNewsSitemap(@Param("from") LocalDateTime from, @Param("now") LocalDateTime now);

    Page<PostJpa> findByDeletedAtIsNull(Pageable pageable);

    long countByStatusAndDeletedAtIsNull(PostStatus status);

    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM PostJpa p WHERE p.deletedAt IS NULL")
    long sumViewCount();

    /**
     * Tăng bộ đếm bằng UPDATE nguyên tử. Đọc rồi ghi lại ở tầng ứng dụng sẽ mất
     * lượt xem khi nhiều người mở bài cùng lúc.
     */
    @Modifying
    @Query("UPDATE PostJpa p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.postId = :postId")
    void incrementViewCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE PostJpa p SET p.shareCount = COALESCE(p.shareCount, 0) + 1 WHERE p.postId = :postId")
    void incrementShareCount(@Param("postId") Long postId);
}
