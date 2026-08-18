package com.g42.platform.gms.marketing.itempost.infrastructure.repository;

import com.g42.platform.gms.marketing.itempost.domain.ItemPostStatus;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostJpa;
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

public interface ItemPostJpaRepo extends JpaRepository<ItemPostJpa, Long>, JpaSpecificationExecutor<ItemPostJpa> {

    Optional<ItemPostJpa> findBySlugAndDeletedAtIsNull(String slug);

    boolean existsBySlug(String slug);

    Optional<ItemPostJpa> findByItemPostIdAndDeletedAtIsNull(Long itemPostId);

    /** Bài viết phụ tùng gắn với một mặt hàng — dùng cho điểm vào từ /warehouse-management. */
    List<ItemPostJpa> findByCatalogItemIdAndDeletedAtIsNull(Integer catalogItemId);

    /** Tra slug hàng loạt cho trang danh sách công khai — tránh N+1 theo từng dòng sản phẩm. */
    List<ItemPostJpa> findByCatalogItemIdInAndStatusAndDeletedAtIsNull(List<Integer> catalogItemIds, ItemPostStatus status);

    /** Các catalog item đã có bài viết (bất kể trạng thái) — dùng để loại trừ khi backfill. */
    @Query("SELECT DISTINCT p.catalogItemId FROM ItemPostJpa p WHERE p.deletedAt IS NULL")
    List<Integer> findDistinctCatalogItemIdsWithPost();

    /** Bài tới hạn đăng — job nền gọi mỗi phút. */
    List<ItemPostJpa> findByStatusAndScheduledAtLessThanEqualAndDeletedAtIsNull(ItemPostStatus status, LocalDateTime moment);

    /**
     * Bài liên quan: cùng danh mục hoặc trùng ít nhất một tag, xếp theo số tag
     * trùng giảm dần rồi tới ngày đăng. Giữ người đọc ở lại thêm một bài nữa.
     */
    @Query("""
            SELECT p FROM ItemPostJpa p
            LEFT JOIN p.tags t
            WHERE p.itemPostId <> :itemPostId
              AND p.deletedAt IS NULL
              AND p.status = com.g42.platform.gms.marketing.itempost.domain.ItemPostStatus.PUBLISHED
              AND p.publishedAt <= :now
              AND (:categoryId IS NULL AND :hasTags = FALSE
                   OR p.category.categoryId = :categoryId
                   OR t.tagId IN :tagIds)
            GROUP BY p
            ORDER BY COUNT(t.tagId) DESC, p.publishedAt DESC
            """)
    List<ItemPostJpa> findRelated(@Param("itemPostId") Long itemPostId,
                              @Param("categoryId") Integer categoryId,
                              @Param("tagIds") List<Integer> tagIds,
                              @Param("hasTags") boolean hasTags,
                              @Param("now") LocalDateTime now,
                              Pageable pageable);

    /** Nguồn cho sitemap — chỉ bài công khai và cho phép index. */
    @Query("""
            SELECT p FROM ItemPostJpa p
            WHERE p.deletedAt IS NULL
              AND p.status = com.g42.platform.gms.marketing.itempost.domain.ItemPostStatus.PUBLISHED
              AND p.publishedAt <= :now
              AND (p.allowIndex IS NULL OR p.allowIndex = TRUE)
            ORDER BY p.publishedAt DESC
            """)
    List<ItemPostJpa> findIndexablePublished(@Param("now") LocalDateTime now, Pageable pageable);

    Page<ItemPostJpa> findByDeletedAtIsNull(Pageable pageable);

    long countByStatusAndDeletedAtIsNull(ItemPostStatus status);

    @Query("SELECT COALESCE(SUM(p.viewCount), 0) FROM ItemPostJpa p WHERE p.deletedAt IS NULL")
    long sumViewCount();

    /**
     * Tăng bộ đếm bằng UPDATE nguyên tử. Đọc rồi ghi lại ở tầng ứng dụng sẽ mất
     * lượt xem khi nhiều người mở bài cùng lúc.
     */
    @Modifying
    @Query("UPDATE ItemPostJpa p SET p.viewCount = COALESCE(p.viewCount, 0) + 1 WHERE p.itemPostId = :itemPostId")
    void incrementViewCount(@Param("itemPostId") Long itemPostId);

    @Modifying
    @Query("UPDATE ItemPostJpa p SET p.shareCount = COALESCE(p.shareCount, 0) + 1 WHERE p.itemPostId = :itemPostId")
    void incrementShareCount(@Param("itemPostId") Long itemPostId);
}
