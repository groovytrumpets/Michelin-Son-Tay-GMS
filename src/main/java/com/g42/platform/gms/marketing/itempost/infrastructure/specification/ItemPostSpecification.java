package com.g42.platform.gms.marketing.itempost.infrastructure.specification;

import com.g42.platform.gms.marketing.itempost.domain.ItemPostStatus;
import com.g42.platform.gms.marketing.itempost.infrastructure.entity.ItemPostJpa;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/** Bộ lọc dùng chung cho cả danh sách công khai lẫn màn hình quản trị. */
public final class ItemPostSpecification {

    private ItemPostSpecification() {
    }

    public static Specification<ItemPostJpa> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /** Chỉ bài thực sự đang hiển thị: đã đăng và đã tới giờ đăng. */
    public static Specification<ItemPostJpa> publiclyVisible(LocalDateTime now) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), ItemPostStatus.PUBLISHED),
                cb.isNotNull(root.get("publishedAt")),
                cb.lessThanOrEqualTo(root.get("publishedAt"), now)
        );
    }

    public static Specification<ItemPostJpa> hasStatus(ItemPostStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<ItemPostJpa> hasCategorySlug(String categorySlug) {
        if (categorySlug == null || categorySlug.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.join("category", JoinType.INNER).get("slug"), categorySlug);
    }

    public static Specification<ItemPostJpa> hasCategoryId(Integer categoryId) {
        if (categoryId == null) return null;
        return (root, query, cb) -> cb.equal(root.join("category", JoinType.INNER).get("categoryId"), categoryId);
    }

    public static Specification<ItemPostJpa> hasCatalogItemId(Integer catalogItemId) {
        if (catalogItemId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("catalogItemId"), catalogItemId);
    }

    /**
     * Lọc theo tag bằng truy vấn con EXISTS thay vì JOIN.
     *
     * <p>JOIN vào bảng nối sẽ nhân bản ghi khi bài có nhiều tag, kéo theo phải
     * bật DISTINCT và làm sai luôn tổng số bản ghi dùng để phân trang.
     */
    public static Specification<ItemPostJpa> hasTagSlug(String tagSlug) {
        if (tagSlug == null || tagSlug.isBlank()) return null;
        return (root, query, cb) -> {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<ItemPostJpa> subPost = sub.from(ItemPostJpa.class);
            Join<Object, Object> subTag = subPost.join("tags", JoinType.INNER);
            sub.select(cb.literal(1));
            sub.where(cb.equal(subPost, root), cb.equal(subTag.get("slug"), tagSlug));
            return cb.exists(sub);
        };
    }

    /**
     * Thứ tự mặc định của danh sách công khai: bài ghim lên đầu (số nhỏ trước),
     * phần còn lại xếp theo ngày đăng mới nhất.
     */
    public static Specification<ItemPostJpa> defaultPublicOrder() {
        return (root, query, cb) -> {
            // Truy vấn đếm bản ghi cho phân trang không được phép có ORDER BY.
            if (query != null && !isCountQuery(query)) {
                Expression<Integer> unpinnedLast = cb.<Integer>selectCase()
                        .when(cb.isNull(root.get("pinnedOrder")), 1)
                        .otherwise(0)
                        .as(Integer.class);
                query.orderBy(
                        cb.asc(unpinnedLast),
                        cb.asc(root.get("pinnedOrder")),
                        cb.desc(root.get("publishedAt")));
            }
            return cb.conjunction();
        };
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }

    public static Specification<ItemPostJpa> isFeatured(Boolean featured) {
        if (featured == null) return null;
        return (root, query, cb) -> featured
                ? cb.isTrue(root.get("isFeatured"))
                : cb.or(cb.isFalse(root.get("isFeatured")), cb.isNull(root.get("isFeatured")));
    }

    public static Specification<ItemPostJpa> keyword(String q) {
        if (q == null || q.isBlank()) return null;
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("excerpt")), pattern),
                cb.like(cb.lower(root.get("slug")), pattern)
        );
    }

    public static Specification<ItemPostJpa> authoredBy(Integer staffId) {
        if (staffId == null) return null;
        return (root, query, cb) -> cb.equal(root.join("author", JoinType.INNER).get("staffId"), staffId);
    }
}
