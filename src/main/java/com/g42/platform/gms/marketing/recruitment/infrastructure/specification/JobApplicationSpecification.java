package com.g42.platform.gms.marketing.recruitment.infrastructure.specification;

import com.g42.platform.gms.marketing.recruitment.domain.ApplicationStatus;
import com.g42.platform.gms.marketing.recruitment.infrastructure.entity.JobApplicationJpa;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/** Bộ lọc màn hình hồ sơ ứng tuyển. */
public final class JobApplicationSpecification {

    private JobApplicationSpecification() {
    }

    public static Specification<JobApplicationJpa> hasStatus(ApplicationStatus status) {
        if (status == null) return null;
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<JobApplicationJpa> hasJob(Long jobId) {
        if (jobId == null) return null;
        return (root, query, cb) -> cb.equal(root.join("job", JoinType.INNER).get("jobId"), jobId);
    }

    public static Specification<JobApplicationJpa> createdFrom(LocalDateTime from) {
        if (from == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<JobApplicationJpa> createdTo(LocalDateTime to) {
        if (to == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    public static Specification<JobApplicationJpa> keyword(String q) {
        if (q == null || q.isBlank()) return null;
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("code")), pattern)
        );
    }
}
