package com.g42.platform.gms.marketing.recruitment.infrastructure.specification;

import com.g42.platform.gms.marketing.recruitment.domain.EmploymentType;
import com.g42.platform.gms.marketing.recruitment.domain.JobStatus;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobPositionJpa;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/** Bộ lọc dùng chung cho cả danh sách công khai lẫn màn hình quản trị. */
public final class JobPositionSpecification {

    private JobPositionSpecification() {
    }

    public static Specification<JobPositionJpa> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /** Tin khách nhìn thấy: đang tuyển hoặc đã đóng (đóng vẫn xem được nội dung). */
    public static Specification<JobPositionJpa> publiclyListed() {
        return (root, query, cb) -> root.get("status")
                .in(List.of(JobStatus.PUBLISHED, JobStatus.CLOSED));
    }

    public static Specification<JobPositionJpa> hasStatus(JobStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<JobPositionJpa> hasDepartment(String department) {
        if (department == null || department.isBlank()) return null;
        return (root, query, cb) -> cb.equal(root.get("department"), department.trim());
    }

    public static Specification<JobPositionJpa> hasEmploymentType(EmploymentType type) {
        if (type == null) return null;
        return (root, query, cb) -> cb.equal(root.get("employmentType"), type);
    }

    public static Specification<JobPositionJpa> keyword(String q) {
        if (q == null || q.isBlank()) return null;
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("summary")), pattern),
                cb.like(cb.lower(root.get("department")), pattern),
                cb.like(cb.lower(root.get("slug")), pattern)
        );
    }

    /**
     * Thứ tự ngoài trang khách: tin nổi bật lên đầu, rồi tới thứ tự người cấu
     * hình tự đặt, cuối cùng mới tới ngày đăng.
     *
     * <p>Đặt ORDER BY bằng Criteria API thay vì {@code Sort} vì cần biểu thức
     * CASE để đẩy tin không nổi bật xuống dưới — {@code Sort} không diễn đạt
     * được điều đó (xem thêm ghi chú tương tự ở PostSpecification).
     */
    public static Specification<JobPositionJpa> defaultPublicOrder() {
        return (root, query, cb) -> {
            // Truy vấn đếm bản ghi cho phân trang không được phép có ORDER BY.
            if (query != null && !isCountQuery(query)) {
                Expression<Integer> notFeaturedLast = cb.<Integer>selectCase()
                        .when(cb.isTrue(root.get("isFeatured")), 0)
                        .otherwise(1)
                        .as(Integer.class);
                // Tin đã đóng luôn nằm dưới tin còn tuyển, bất kể nổi bật hay không.
                Expression<Integer> closedLast = cb.<Integer>selectCase()
                        .when(cb.equal(root.get("status"), JobStatus.PUBLISHED), 0)
                        .otherwise(1)
                        .as(Integer.class);
                query.orderBy(
                        cb.asc(closedLast),
                        cb.asc(notFeaturedLast),
                        cb.asc(root.get("displayOrder")),
                        cb.desc(root.get("publishedAt")));
            }
            return cb.conjunction();
        };
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return Long.class.equals(resultType) || long.class.equals(resultType);
    }
}
